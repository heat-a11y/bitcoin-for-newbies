# ── Bitcoin Widget ProGuard / R8 Rules ──────────────────────────────

# Keep the AppWidgetProvider entry point (reflected by the OS)
-keep class com.bitcoinwidget.BitcoinWidgetProvider { *; }

# Keep the WorkManager CoroutineWorker (used via reflection by WorkManager)
-keep class com.bitcoinwidget.BitcoinDataWorker { *; }

# Keep the MainActivity launcher
-keep class com.bitcoinwidget.MainActivity { *; }

# Keep JSON parsing (org.json is part of the platform, keep our usage)
-keep class com.bitcoinwidget.BitcoinApiClient { *; }

# WorkManager – keep all worker classes
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
