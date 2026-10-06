package com.pratul.expensesmanager.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.billing.TipJar
import com.pratul.expensesmanager.data.AppStyle
import com.pratul.expensesmanager.data.ThemeMode
import com.pratul.expensesmanager.data.defaultEmojiFor
import com.pratul.expensesmanager.ui.components.AddExpenseDialog
import com.pratul.expensesmanager.ui.components.ConfettiBurst
import com.pratul.expensesmanager.ui.components.LocalCatalog
import com.pratul.expensesmanager.ui.components.LocalQuickAdd
import com.pratul.expensesmanager.ui.components.LocalSetBill
import com.pratul.expensesmanager.ui.screens.LocalTipJar
import com.pratul.expensesmanager.ui.screens.AccountScreen
import com.pratul.expensesmanager.ui.screens.CompareScreen
import com.pratul.expensesmanager.ui.screens.LendScreen
import com.pratul.expensesmanager.ui.screens.LoanDialog
import com.pratul.expensesmanager.ui.screens.MonthScreen
import com.pratul.expensesmanager.ui.screens.SpendFinderDialog
import com.pratul.expensesmanager.ui.screens.YearlyScreen
import com.pratul.expensesmanager.util.formatMoneyWhole
import kotlinx.coroutines.launch

/** What "New spend" should start with. */
data class AddRequest(val category: String?, val item: String?)

enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    Month("Month", R.drawable.ic_pie_chart),
    Compare("Compare", R.drawable.ic_compare),
    Yearly("Yearly", R.drawable.ic_bar_chart),
    Lend("Lend", R.drawable.ic_swap),
    Account("Profile", R.drawable.ic_person)
}

@Composable
fun ExpenseApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    appStyle: AppStyle,
    onAppStyleChange: (AppStyle) -> Unit
) {
    val context = LocalContext.current
    val state = remember { AppState(context) }
    var tab by rememberSaveable { mutableStateOf(Tab.Month) }
    // Non-null while "New spend" is open; may carry a category/item chosen by a swipe
    var addRequest by remember { mutableStateOf<AddRequest?>(null) }
    var showSpendFinder by remember { mutableStateOf(false) }
    var addingLoan by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Tips through Google Play; connecting at start also completes tips approved while the app was closed
    val tipJar = remember { TipJar(context) }
    DisposableEffect(tipJar) {
        tipJar.connect()
        onDispose { tipJar.close() }
    }
    LaunchedEffect(tipJar.completedTips) {
        if (tipJar.completedTips > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            confetti++
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("Thank you so much for the tip! ☕💜")
        }
    }
    LaunchedEffect(tipJar.problem) {
        tipJar.problem?.let {
            snackbar.showSnackbar(it)
            tipJar.problem = null
        }
    }

    CompositionLocalProvider(
        LocalCatalog provides state.catalog,
        LocalQuickAdd provides { category, item -> addRequest = AddRequest(category, item) },
        LocalQuickAdd provides { category, item -> addRequest = AddRequest(category, item) },
        LocalSetBill provides state::setBill,
        LocalTipJar provides tipJar
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        for (t in Tab.entries) {
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(painterResource(t.icon), contentDescription = null) },
                                label = { Text(t.label, maxLines = 1, softWrap = false) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                },
                floatingActionButton = {
                    // Spend finder above "+", bottom right, on This month and Compare only
                    if (tab == Tab.Month || tab == Tab.Compare) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = { showSpendFinder = true },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(painterResource(R.drawable.ic_date_range), contentDescription = "Spend finder: any period")
                            }
                            FloatingActionButton(
                                onClick = { addRequest = AddRequest(null, null) },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = "Add expense")
                            }
                        }
                    }
                    // On Borrow & Lend, "+" adds a record
                    if (tab == Tab.Lend) {
                        FloatingActionButton(
                            onClick = { addingLoan = true },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Icon(painterResource(R.drawable.ic_add), contentDescription = "Add borrow or lend")
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.End
            ) { innerPadding ->
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 20 }) togetherWith fadeOut(tween(120))
                    },
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    label = "tabs"
                ) { shown ->
                    when (shown) {
                        Tab.Month -> MonthScreen(state.expenses, onDelete = state::deleteExpense)
                        Tab.Compare -> CompareScreen(state.expenses)
                        Tab.Yearly -> YearlyScreen(state.expenses)
                        Tab.Lend -> LendScreen(state, onAdd = { addingLoan = true })
                        Tab.Account -> AccountScreen(state, themeMode, onThemeModeChange, appStyle, onAppStyleChange)
                    }
                }
            }

            // Drawn over everything but doesn't block touches
            ConfettiBurst(confetti)
        }

        if (addingLoan) {
            LoanDialog(
                initial = null,
                people = state.loans.map { it.person.trim() }.distinct(),
                onDismiss = { addingLoan = false },
                onSave = {
                    state.saveLoan(it)
                    addingLoan = false
                    confetti++
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            )
        }

        if (showSpendFinder) {
            SpendFinderDialog(
                expenses = state.expenses,
                catalog = state.catalog,
                onDelete = state::deleteExpense,
                onClose = { showSpendFinder = false }
            )
        }

        val request = addRequest
        if (request != null) key(request) {
            AddExpenseDialog(
                catalog = state.catalog,
                expenses = state.expenses,
                onAddCategory = state::addCategory,
                onAddItem = state::addItem,
                onDismiss = { addRequest = null },
                initialCategory = request.category,
                initialItem = request.item,
                onSave = { expense ->
                    state.addExpense(expense)
                    addRequest = null
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    confetti++
                    val emoji = state.catalog.firstOrNull { it.name == expense.category }?.emoji
                        ?: defaultEmojiFor(expense.category)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(
                            "$emoji ${formatMoneyWhole(expense.amount)} on ${expense.item.ifBlank { expense.category }} saved ✅"
                        )
                    }
                }
            )
        }
    }
}
