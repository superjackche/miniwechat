package com.example.nearbychater

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nearbychater.ui.ChatScreen
import com.example.nearbychater.ui.ConversationListScreen
import com.example.nearbychater.ui.LogsScreen
import com.example.nearbychater.ui.SettingsScreen
import com.example.nearbychater.ui.state.ChatViewModel
import com.example.nearbychater.ui.state.SettingsViewModel

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LOGS = "logs"
private const val ROUTE_CHAT = "chat/{conversationId}"
private const val ARG_CONVERSATION_ID = "conversationId"
private const val NAV_ANIMATION_MILLIS = 200

@Composable
internal fun NearbyChaterNavHost(
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = navAnimationSpec()
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = navAnimationSpec()
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = navAnimationSpec()
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = navAnimationSpec()
            )
        }
    ) {
        composable(ROUTE_HOME) {
            ConversationListScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = chatViewModel,
                onConversationSelected = { conversationId ->
                    navController.navigate("chat/$conversationId") { launchSingleTop = true }
                },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onOpenLogs = { navController.navigate(ROUTE_LOGS) }
            )
        }

        composable(
            route = ROUTE_CHAT,
            arguments = listOf(navArgument(ARG_CONVERSATION_ID) { type = NavType.StringType })
        ) { entry ->
            ChatScreen(
                modifier = Modifier.fillMaxSize(),
                conversationId = entry.arguments?.getString(ARG_CONVERSATION_ID),
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onOpenLogs = { navController.navigate(ROUTE_LOGS) },
                viewModel = chatViewModel
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = settingsViewModel,
                selfMemberId = chatViewModel.selfMemberId
            )
        }

        composable(ROUTE_LOGS) {
            LogsScreen(modifier = Modifier.fillMaxSize(), viewModel = settingsViewModel)
        }
    }
}

private fun navAnimationSpec() = tween<IntOffset>(
    durationMillis = NAV_ANIMATION_MILLIS,
    easing = FastOutSlowInEasing
)
