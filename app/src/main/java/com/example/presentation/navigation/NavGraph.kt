package com.example.presentation.navigation

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.MindMateApplication
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.OnboardingViewModel
import com.example.presentation.viewmodel.ViewModelFactory

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val CHECK_IN = "check_in"
    const val COMPANION = "companion"
    const val TOOLKIT = "toolkit"
    const val CRISIS = "crisis"
}

data class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MindMateNavGraph(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as MindMateApplication
    val container = app.container

    // Shared Onboarding state to decide starting route
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = ViewModelFactory(
            application = app,
            wellnessRepository = container.wellnessRepository,
            analyzeJournalUseCase = container.analyzeJournalUseCase,
            detectCrisisUseCase = container.detectCrisisUseCase,
            getWellnessScoreUseCase = container.getWellnessScoreUseCase,
            examTypeProvider = { "JEE" } // Initial placeholder, overridden
        )
    )

    val isOnboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()
    val examType by onboardingViewModel.examType.collectAsState()

    // Setup factory that dynamically references the saved examType
    val factory = remember(examType) {
        ViewModelFactory(
            application = app,
            wellnessRepository = container.wellnessRepository,
            analyzeJournalUseCase = container.analyzeJournalUseCase,
            detectCrisisUseCase = container.detectCrisisUseCase,
            getWellnessScoreUseCase = container.getWellnessScoreUseCase,
            examTypeProvider = { examType }
        )
    }

    val startDestination = if (isOnboardingCompleted) Routes.DASHBOARD else Routes.ONBOARDING

    val navItems = listOf(
        NavigationItem(Routes.DASHBOARD, "Dashboard", Icons.Default.Home, Icons.Default.Home),
        NavigationItem(Routes.CHECK_IN, "Check-In", Icons.Default.Create, Icons.Default.Create),
        NavigationItem(Routes.COMPANION, "Companion", Icons.Default.Send, Icons.Default.Send),
        NavigationItem(Routes.TOOLKIT, "Toolkit", Icons.Default.Build, Icons.Default.Build),
        NavigationItem(Routes.CRISIS, "Support", Icons.Default.Warning, Icons.Default.Warning)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != Routes.ONBOARDING

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onOnboardingCompleted = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToCheckIn = { navController.navigate(Routes.CHECK_IN) },
                    onNavigateToCompanion = { navController.navigate(Routes.COMPANION) },
                    onNavigateToToolkit = { navController.navigate(Routes.TOOLKIT) },
                    onNavigateToCrisis = { navController.navigate(Routes.CRISIS) }
                )
            }

            composable(Routes.CHECK_IN) {
                CheckInScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToCompanion = { navController.navigate(Routes.COMPANION) }
                )
            }

            composable(Routes.COMPANION) {
                CompanionScreen(
                    viewModel = viewModel(factory = factory)
                )
            }

            composable(Routes.TOOLKIT) {
                ToolkitScreen()
            }

            composable(Routes.CRISIS) {
                CrisisScreen()
            }
        }
    }
}
