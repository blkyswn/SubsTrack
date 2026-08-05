package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarRatingBar(
    rating: Double,
    maxStars: Int = 5,
    onRatingChanged: ((Double) -> Unit)? = null,
    starSize: Dp = 28.dp,
    starColor: Color = Color(0xFFFFC107) // Amber/Gold color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..maxStars) {
                val starValue = i.toDouble()
                val icon = when {
                    rating >= starValue -> Icons.Filled.Star
                    rating >= starValue - 0.5 -> Icons.Filled.StarHalf
                    else -> Icons.Outlined.StarBorder
                }

                val modifier = if (onRatingChanged != null) {
                    Modifier
                        .size(starSize)
                        .clickable {
                            val newRating = if (rating == starValue) {
                                starValue - 0.5
                            } else if (rating == starValue - 0.5) {
                                starValue - 1.0
                            } else {
                                starValue
                            }
                            onRatingChanged(newRating.coerceIn(0.0, maxStars.toDouble()))
                        }
                        .testTag("star_rating_button_$i")
                } else {
                    Modifier.size(starSize)
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "Star $i",
                    tint = starColor,
                    modifier = modifier
                )
            }
        }

        if (onRatingChanged != null) {
            Spacer(modifier = Modifier.width(8.dp))
            if (rating == 0.0) {
                Text(
                    text = "Not Rated",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("rating_status_text")
                )
            } else {
                Text(
                    text = "$rating",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("rating_status_text")
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = { onRatingChanged(0.0) },
                    modifier = Modifier.testTag("clear_rating_button")
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }
    }
}

