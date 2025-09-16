@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.pizzamania.screens.builder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pizzamania.data.model.Topping
import com.pizzamania.ui.widgets.MenuCustomizeTopBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun PizzaBuilderScreen(
    navController: NavController,
    branchId: String,
    vm: PizzaBuilderViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    val catalog = remember {
        listOf(
            Topping("che",  "Cheese",     300.0, "🧀", pieces = 180),
            Topping("mush", "Mushrooms",  180.0, "🍄", pieces = 42),
            Topping("pep",  "Pepperoni",  350.0, "🍖", pieces = 36),
            Topping("olv",  "Olives",     150.0, "🫒", pieces = 34),
        )
    }

    val basePrice = 1650.0
    var selected by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var particles by remember { mutableStateOf(listOf<Particle>()) }

    val totalPrice by derivedStateOf {
        basePrice + catalog.filter { selected[it.id] == true }.sumOf { it.price }
    }

    Scaffold(
        topBar = {
            MenuCustomizeTopBar(
                title = "Customize",
                selectedTabIndex = 1,
                onBack = { navController.popBackStack() },
                onSelectMenu = { navController.navigate("menu/$branchId") },
                onSelectCustomize = { /* here */ }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(catalog) { t ->
                            val on = selected[t.id] == true
                            FilterChip(
                                selected = on,
                                onClick = {
                                    val now = selected + (t.id to !on)
                                    selected = now
                                    if (!on) {
                                        val newOnes = scatterFor(t)
                                        particles = particles + newOnes
                                        launchFalls(scope, newOnes)
                                    } else {
                                        particles = particles.filterNot { p -> p.toppingId == t.id }
                                    }
                                },
                                label = { Text("${t.emoji} ${t.name}") },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total: Rs. ${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = {
                                val chosen = catalog.filter { selected[it.id] == true }
                                scope.launch {
                                    vm.addCustomPizza(
                                        branchId = branchId,
                                        basePrice = basePrice,
                                        selections = chosen
                                    )
                                    navController.navigate("cart/$branchId")
                                }
                            }
                        ) { Text("Add to cart") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                PizzaCanvas(
                    particles = particles,
                    diameter = 340.dp,
                    showCheeseBase = (selected["che"] == true)
                )
            }
        }
    }
}

/* ---------------- particles + animation ---------------- */

private data class Particle(
    val id: String,
    val toppingId: String,
    val emoji: String,
    /** final position in PX relative to the CENTER (0,0) */
    val finalOffsetPx: Offset,
    val renderSp: Float,                 // final font size in sp (already includes scale)
    val yAnim: Animatable<Float, *>
)

private fun launchFalls(scope: CoroutineScope, list: List<Particle>) {
    list.forEachIndexed { idx, p ->
        scope.launch {
            delay(idx * 12L)
            p.yAnim.snapTo(p.finalOffsetPx.y - 360f) // start well above
            p.yAnim.animateTo(
                targetValue = p.finalOffsetPx.y,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }
}

/* ---------------- drawing ---------------- */

@Composable
private fun PizzaCanvas(
    particles: List<Particle>,
    diameter: Dp,
    showCheeseBase: Boolean
) {
    val dPx = with(LocalDensity.current) { diameter.toPx() }
    val r = dPx / 2f
    val cx = r
    val cy = r

    val dough = Color(0xFFF1C27D)
    val cheeseBase = Color(0xFFF6D86A)

    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // dough / cheese base
            drawCircle(dough, radius = r)
            if (showCheeseBase) drawCircle(cheeseBase, radius = r * 0.92f)

            // subtle bake specks
            val dot = dough.copy(alpha = 0.35f)
            repeat(24) {
                val a = Random.nextFloat() * (2 * PI).toFloat()
                val rr = Random.nextFloat() * r * 0.6f
                val x = cx + cos(a) * rr
                val y = cy + sin(a) * rr
                drawOval(dot, topLeft = Offset(x, y), size = Size(8f, 5f))
            }
        }

        // toppings (emoji)
        particles.forEach { p ->
            val y by p.yAnim.asState()
            Text(
                text = p.emoji,
                fontSize = p.renderSp.sp,
                modifier = Modifier
                    // shift from center to top-left for actual drawing
                    .absoluteOffset(
                        x = (p.finalOffsetPx.x + cx).pxToDp(),
                        y = (y + cy).pxToDp()
                    ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ---------------- helpers ---------------- */

/** Poisson-like rejection sampling to keep a minimum distance between points. */
private fun samplePoints(
    count: Int,
    usableRadius: Float,
    minDistPx: Float
): List<Offset> {
    val pts = mutableListOf<Offset>()
    val maxTries = count * 60
    var tries = 0
    fun ok(p: Offset) = pts.none { (it.x - p.x) * (it.x - p.x) + (it.y - p.y) * (it.y - p.y) < minDistPx * minDistPx }

    while (pts.size < count && tries < maxTries) {
        tries++
        val angle = Random.nextFloat() * (2f * PI).toFloat()
        val dist = usableRadius * sqrt(Random.nextFloat()) // uniform over disk
        val p = Offset(cos(angle) * dist, sin(angle) * dist)
        if (ok(p)) pts += p
    }
    // If we didn’t hit the target count, fill the rest without spacing so we still show something
    while (pts.size < count) {
        val a = Random.nextFloat() * (2f * PI).toFloat()
        val d = usableRadius * sqrt(Random.nextFloat())
        pts += Offset(cos(a) * d, sin(a) * d)
    }
    return pts
}

private fun scatterFor(t: Topping): List<Particle> {
    val diameterPx = 340f
    val usableRadius = diameterPx / 2f * 0.90f

    // Per-topping visual config (size + spacing)
    val (baseSp, minDistPx, pieces) = when (t.id) {
        "che"  -> Triple(13f,  10f, maxOf(t.pieces, 180))   // many small bits
        "mush" -> Triple(26f,  20f, t.pieces)               // bigger mushrooms
        "pep"  -> Triple(28f,  22f, t.pieces)               // chunky pepperoni
        "olv"  -> Triple(24f,  18f, t.pieces)               // olives
        else   -> Triple(24f,  18f, t.pieces)
    }

    val positions =
        if (t.id == "che") {
            // cheese covered uniformly using golden-angle spiral then slightly jittered
            val golden = 137.50776405003785 * (PI / 180.0)
            List(pieces) { i ->
                val k = (i + 0.5) / pieces
                val r = usableRadius * sqrt(k.toFloat())
                val theta = (i * golden).toFloat()
                val jitter = 4f
                Offset(
                    r * cos(theta) + Random.nextFloat() * jitter - jitter / 2f,
                    r * sin(theta) + Random.nextFloat() * jitter - jitter / 2f
                )
            }
        } else {
            samplePoints(pieces, usableRadius, minDistPx)
        }

    return positions.mapIndexed { idx, pos ->
        val scale = when (t.id) {
            "che"  -> 0.9f + Random.nextFloat() * 0.5f   // 0.9..1.4
            else   -> 1.1f + Random.nextFloat() * 0.6f   // 1.1..1.7
        }
        Particle(
            id = "${t.id}_${idx}_${System.nanoTime()}",
            toppingId = t.id,
            emoji = t.emoji,
            finalOffsetPx = pos,                 // relative to center
            renderSp = baseSp * scale,
            yAnim = Animatable(pos.y)            // animate from above in launchFalls()
        )
    }
}

@Composable
private fun Float.pxToDp(): Dp {
    val density = LocalDensity.current
    return with(density) { toDp() }
}
