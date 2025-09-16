package com.pizzamania.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class Promo(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    @DrawableRes val imageRes: Int
)

@Composable
fun PromoStrip(
    promos: List<Promo>,
    onPromoClick: (Promo) -> Unit
) {
    if (promos.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(promos) { p ->
            Card(
                onClick = { onPromoClick(p) },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.size(width = 280.dp, height = 140.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = p.imageRes),
                        contentDescription = p.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            p.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (p.subtitle != null)
                            Text(p.subtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
