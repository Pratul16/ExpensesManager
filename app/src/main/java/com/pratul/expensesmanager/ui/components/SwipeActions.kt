package com.pratul.expensesmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import kotlinx.coroutines.launch

/**
 * Opens "New spend" with a category and/or item already chosen. Provided by the app's root so any
 * screen can offer swipe-right-to-add.
 */
val LocalQuickAdd = compositionLocalOf<(category: String?, item: String?) -> Unit> { { _, _ -> } }

/**
 * A row that can be swiped right (add, green) and/or left (delete, red). The row always slides back;
 * the caller decides what happens (usually a dialog).
 */
@Composable
fun SwipeActions(
    onSwipeRight: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    corner: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(corner)

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = onSwipeRight != null,
        enableDismissFromEndToStart = onSwipeLeft != null,
        onDismiss = { direction ->
            scope.launch { state.reset() }
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipeRight?.invoke()
                SwipeToDismissBoxValue.EndToStart -> onSwipeLeft?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = {
            val direction = state.dismissDirection
            val adding = direction == SwipeToDismissBoxValue.StartToEnd
            if (direction == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (adding) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = if (adding) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val color = if (adding) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                if (adding) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = color)
                    Spacer(Modifier.width(8.dp))
                    Text("Add spend", color = color, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Delete", color = color, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = color)
                }
            }
        },
        content = content
    )
}
