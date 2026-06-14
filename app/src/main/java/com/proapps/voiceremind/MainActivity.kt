package com.proapps.voiceremind

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.provider.CalendarContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.proapps.voiceremind.geofence.StoreGeofenceManager
import com.proapps.voiceremind.expenses.ExpenseLogCommandParser
import com.proapps.voiceremind.expenses.ExpenseLogStore
import com.proapps.voiceremind.expenses.ExpenseWeeklyReportScheduler
import com.proapps.voiceremind.medication.MedicationReminderNotifier
import com.proapps.voiceremind.medication.MedicationReminderParser
import com.proapps.voiceremind.medication.MedicationReminderScheduler
import com.proapps.voiceremind.medication.MedicationLogShareHelper
import com.proapps.voiceremind.medication.MedicationLogStore
import com.proapps.voiceremind.parking.ParkingControlCommandParser
import com.proapps.voiceremind.parking.ParkingControlScheduler
import com.proapps.voiceremind.parcel.ParcelPickupCommandParser
import com.proapps.voiceremind.parcel.ParcelPickupGeofenceManager
import com.proapps.voiceremind.sauna.SaunaTimerCommandParser
import com.proapps.voiceremind.sauna.SaunaTimerScheduler
import com.proapps.voiceremind.sahko.SahkoVahtiCommandParser
import com.proapps.voiceremind.sahko.SahkoVahtiScheduler
import com.proapps.voiceremind.waste.WastePickupCommandParser
import com.proapps.voiceremind.waste.WastePickupScheduler
import com.proapps.voiceremind.child.ChildCareCommandParser
import com.proapps.voiceremind.child.ChildCareCommand
import com.proapps.voiceremind.child.ChildCareLogStore
import com.proapps.voiceremind.child.ChildEventType
import com.proapps.voiceremind.messaging.MessageReminderNotifier
import com.proapps.voiceremind.messaging.MessageReminderScheduler
import com.proapps.voiceremind.security.SensitiveDataVault
import com.proapps.voiceremind.weather.OpenMeteoClient
import com.proapps.voiceremind.weather.WeatherAlertNotifier
import com.proapps.voiceremind.weather.WeatherAlertScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

private const val STATE_INPUT_TEXT = "state_input_text"
private const val STATE_PENDING_TITLE = "state_pending_title"
private const val STATE_PENDING_DATETIME = "state_pending_datetime"
private const val STATE_PENDING_USED_DEFAULT_TIME = "state_pending_used_default_time"
private const val STATE_PENDING_LOCATION = "state_pending_location"
private const val STATE_PENDING_DURATION = "state_pending_duration"
private const val DRAFT_PREFS = "draft_prefs"
private const val DRAFT_INPUT_TEXT = "draft_input_text"
private const val DRAFT_PENDING_TITLE = "draft_pending_title"
private const val DRAFT_PENDING_DATETIME = "draft_pending_datetime"
private const val DRAFT_PENDING_USED_DEFAULT_TIME = "draft_pending_used_default_time"
private const val DRAFT_PENDING_LOCATION = "draft_pending_location"
private const val DRAFT_PENDING_DURATION = "draft_pending_duration"
private const val FIRST_LAUNCH_PREFS = "first_launch_prefs"
private const val FIRST_LAUNCH_PERMISSIONS_REQUESTED = "first_launch_permissions_requested"
private const val FIRST_LAUNCH_PERMISSIONS_DEFERRED = "first_launch_permissions_deferred"
private const val FIRST_LAUNCH_GENTLE_REMINDER_SHOWN = "first_launch_gentle_reminder_shown"
private const val WEATHER_PREFS = "weather_prefs"
private const val WEATHER_FALLBACK_CITY = "weather_fallback_city"
private const val APP_SETTINGS_PREFS = "app_settings_prefs"
private const val DEFAULT_REMINDER_TIME = "default_reminder_time"
private const val SAHKO_NIGHT_START_HOUR = "sahko_night_start_hour"
private const val SAHKO_NIGHT_END_HOUR = "sahko_night_end_hour"
private const val HANDS_FREE_VOICE_CONFIRMATION_ENABLED = "hands_free_voice_confirmation_enabled"
private const val HSL_PACKAGE = "fi.hsl.app"
private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
private const val VOICE_CONFIRM_UTTERANCE_ID = "voice_confirm_utterance"
private const val LIBRARY_CLOSING_HOUR = 20
private const val LIBRARY_REMINDER_TWO_DAYS_MINUTES = 2 * 24 * 60
private const val LIBRARY_REMINDER_ONE_HOUR_MINUTES = 60
private const val TIRE_FROST_CHECK_DAYS = 16
private const val TIRE_FREEZE_THRESHOLD_C = "tire_freeze_threshold_c"
private const val TIRE_SUMMER_DAY = "tire_summer_day"
private const val TIRE_SUMMER_MONTH = "tire_summer_month"
private const val TIRE_WINTER_DAY = "tire_winter_day"
private const val TIRE_WINTER_MONTH = "tire_winter_month"
private const val NIGHT_SILENT_MODE_ENABLED = "night_silent_mode_enabled"
private const val EMOJI_CATEGORIES_ENABLED = "emoji_categories_enabled"
private const val PENDING_STORE_GEO_REQUEST = "pending_store_geo_request"
private const val QUIET_HOURS_START = 22
private const val QUIET_HOURS_END = 7
private const val QUIET_FEEDBACK_VIBRATION_MS = 120L

private enum class PermissionRetryType {
    INITIAL,
    CALENDAR
}

class MainActivity : AppCompatActivity() {

    private lateinit var parser: ReminderParser
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    private lateinit var reminderInput: TextInputEditText
    private lateinit var parsedPreview: TextView
    private lateinit var permissionsMicStatusText: TextView
    private lateinit var permissionsCalendarStatusText: TextView
    private lateinit var permissionsMicStatusRow: View
    private lateinit var permissionsCalendarStatusRow: View
    private lateinit var permissionsMicInfoButton: ImageButton
    private lateinit var permissionsCalendarInfoButton: ImageButton
    private lateinit var weatherFallbackCityText: TextView
    private lateinit var weatherFallbackCityButton: Button
    private lateinit var defaultReminderTimeText: TextView
    private lateinit var defaultReminderTimeButton: Button
    private lateinit var sahkoNightWindowText: TextView
    private lateinit var sahkoNightWindowButton: Button
    private lateinit var handsFreeVoiceStatusText: TextView
    private lateinit var handsFreeVoiceSwitch: SwitchMaterial
    private lateinit var nightSilentModeStatusText: TextView
    private lateinit var nightSilentModeSwitch: SwitchMaterial
    private lateinit var emojiCategoriesStatusText: TextView
    private lateinit var emojiCategoriesSwitch: SwitchMaterial
    private lateinit var tireFreezeThresholdText: TextView
    private lateinit var tireFreezeThresholdButton: Button
    private lateinit var tirePolicyText: TextView
    private lateinit var tirePolicyButton: Button
    private lateinit var previewRouteButton: Button
    private lateinit var editPendingButton: Button
    private lateinit var clearDraftButton: Button
    private lateinit var medicationExportLogButton: Button
    private lateinit var medicationOpenLogButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var openSettingsButton: ImageButton
    private lateinit var pendingActionsRow: View
    private var activeDrawerSection: View? = null

    private var pendingReminder: ParsedReminder? = null
    private var lastClearedDraftSnapshot: DraftSnapshot? = null
    private var pendingVoiceReminder: ParsedReminder? = null
    private var pendingStoreGeoRequest: StoreGeoRequest? = null
    private var pendingParcelGeoRequest: ParcelGeoRequest? = null
    private var isAwaitingVoiceDecision: Boolean = false
    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var busAnnouncer: BusTimeAnnouncer? = null
    private var pendingBusRouteQuery: String? = null
    private lateinit var busRouteText: TextView
    private lateinit var busStopText: TextView
    private lateinit var busTimeText: TextView
    private lateinit var busMinutesText: TextView
    private lateinit var busRealtimeText: TextView
    private lateinit var busSourceText: TextView
    private lateinit var busProgress: View

    private val requestCalendarPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val deniedPermissions = result
            .filterValues { granted -> !granted }
            .keys
            .toList()
        val granted = deniedPermissions.isEmpty()

        if (granted) {
            savePendingReminder()
        } else {
            val isPermanentDenial = deniedPermissions.any { isPermissionPermanentlyDenied(it) }
            val messageType = PermissionDeniedMessageSelector.select(
                microphoneDenied = false,
                calendarDenied = true,
                microphonePermanentlyDenied = false,
                calendarPermanentlyDenied = isPermanentDenial
            )
            val messageRes = messageType?.let { permissionDeniedMessageRes(it) }

            if (messageRes != null) {
                Toast.makeText(this, getString(messageRes), Toast.LENGTH_LONG).show()
                showPermissionDeniedFeedback(
                    messageRes = messageRes,
                    isPermanentDenial = isPermanentDenial,
                    retryType = PermissionRetryType.CALENDAR,
                    retryPermissions = null
                )
            }
        }

        updatePermissionsStatusUi()
    }

    private val requestInitialPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // First-launch request is best-effort: app keeps working with fallbacks.
        val deniedPermissions = result
            .filterValues { granted -> !granted }
            .keys
            .toSet()

        val microphoneDenied = deniedPermissions.contains(Manifest.permission.RECORD_AUDIO)
        val calendarDenied = deniedPermissions.contains(Manifest.permission.READ_CALENDAR) ||
            deniedPermissions.contains(Manifest.permission.WRITE_CALENDAR)
        val microphonePermanentlyDenied = microphoneDenied && isPermissionPermanentlyDenied(Manifest.permission.RECORD_AUDIO)
        val calendarPermanentlyDenied = calendarDenied && (
            (deniedPermissions.contains(Manifest.permission.READ_CALENDAR) && isPermissionPermanentlyDenied(Manifest.permission.READ_CALENDAR)) ||
                (deniedPermissions.contains(Manifest.permission.WRITE_CALENDAR) && isPermissionPermanentlyDenied(Manifest.permission.WRITE_CALENDAR))
            )

        val messageType = PermissionDeniedMessageSelector.select(
            microphoneDenied = microphoneDenied,
            calendarDenied = calendarDenied,
            microphonePermanentlyDenied = microphonePermanentlyDenied,
            calendarPermanentlyDenied = calendarPermanentlyDenied
        )
        val messageRes = messageType?.let { permissionDeniedMessageRes(it) }

        if (messageRes != null) {
            val hasPermanentDenial = microphonePermanentlyDenied || calendarPermanentlyDenied
            showPermissionDeniedFeedback(
                messageRes = messageRes,
                isPermanentDenial = hasPermanentDenial,
                retryType = PermissionRetryType.INITIAL,
                retryPermissions = deniedPermissions.toTypedArray()
            )
        }

        updatePermissionsStatusUi()
    }

    private val requestStoreGeoPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        val pendingStore = pendingStoreGeoRequest
        val pendingParcel = pendingParcelGeoRequest
        val pendingBus = pendingBusRouteQuery

        if (!granted || (pendingStore == null && pendingParcel == null && pendingBus == null)) {
            pendingStoreGeoRequest = null
            pendingParcelGeoRequest = null
            pendingBusRouteQuery = null
            if (!granted) {
                Toast.makeText(this, getString(R.string.parcel_geo_permission_required), Toast.LENGTH_LONG).show()
            }
            return@registerForActivityResult
        }

        pendingStoreGeoRequest = null
        pendingParcelGeoRequest = null
        pendingStore?.let { registerStoreGeoReminder(it) }
        pendingParcel?.let { registerParcelGeoReminder(it) }

        // handle pending bus route query if present
        if (pendingBus != null) {
            pendingBusRouteQuery = null
                try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        busProgress.visibility = View.VISIBLE
                        busAnnouncer?.announceNextBus(pendingBus, loc.latitude, loc.longitude) { departure ->
                            displayBusDeparture(departure, pendingBus)
                        }
                    } else {
                        val msg = "Не удалось получить местоположение."
                        if (isTtsReady) tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "bus_query")
                        else Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val msg = "Не удалось получить местоположение."
                if (isTtsReady) tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "bus_query")
            }
        }
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()

        if (spokenText.isBlank()) {
            Toast.makeText(this, getString(R.string.voice_input_failed), Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        if (isAwaitingVoiceDecision) {
            handleVoiceDecision(spokenText)
            return@registerForActivityResult
        }

        reminderInput.setText(spokenText)

        if (handleSensitiveDataCommand(spokenText)) {
            return@registerForActivityResult
        }

        if (handleSahkoVahtiCommand(spokenText)) {
            return@registerForActivityResult
        }

        if (handleSaunaTimerCommand(spokenText)) {
            return@registerForActivityResult
        }

        if (handleParkingControlCommand(spokenText)) {
            return@registerForActivityResult
        }

        if (handleParcelPickupCommand(spokenText)) {
            return@registerForActivityResult
        }

        if (handleWastePickupCommand(spokenText)) {
            return@registerForActivityResult
        }

        if (handleChildCareCommand(spokenText)) {
            return@registerForActivityResult
        }

        if (handleBusTimeQuery(spokenText)) {
            return@registerForActivityResult
        }

        if (handleExpenseLogCommand(spokenText)) {
            return@registerForActivityResult
        }

        updatePreview(spokenText)

        val prepared = parseReminderWithDomainFallback(spokenText)
        if (prepared == null) {
            return@registerForActivityResult
        }

        if (isHandsFreeVoiceConfirmationEnabled()) {
            askVoiceConfirmation(prepared)
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            Toast.makeText(this, getString(R.string.ocr_failed), Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        processAnnouncementPhoto(bitmap)
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            Toast.makeText(this, getString(R.string.ocr_pick_failed), Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        processAnnouncementPhotoFromUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Always follow device locale. This also clears any previously saved app locale override.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        super.onCreate(savedInstanceState)
        parser = createReminderParser()
        initTextToSpeech()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // init location client and bus announcer
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        busAnnouncer = BusTimeAnnouncer(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        openSettingsButton = findViewById(R.id.openSettingsButton)
        val busQuickButton: ImageButton = findViewById(R.id.busQuickButton)
        busQuickButton.setOnClickListener { showBusDialog() }
        openSettingsButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                openSettingsButton.rotation = 90f * slideOffset
            }

            override fun onDrawerClosed(drawerView: View) {
                openSettingsButton.rotation = 0f
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        reminderInput = findViewById(R.id.reminderInput)
        parsedPreview = findViewById(R.id.parsedPreview)
        permissionsMicStatusText = findViewById(R.id.permissionsMicStatusText)
        permissionsCalendarStatusText = findViewById(R.id.permissionsCalendarStatusText)
        permissionsMicStatusRow = findViewById(R.id.permissionsMicStatusRow)
        permissionsCalendarStatusRow = findViewById(R.id.permissionsCalendarStatusRow)
        permissionsMicInfoButton = findViewById(R.id.permissionsMicInfoButton)
        permissionsCalendarInfoButton = findViewById(R.id.permissionsCalendarInfoButton)
        weatherFallbackCityText = findViewById(R.id.weatherFallbackCityText)
        weatherFallbackCityButton = findViewById(R.id.weatherFallbackCityButton)
        defaultReminderTimeText = findViewById(R.id.defaultReminderTimeText)
        defaultReminderTimeButton = findViewById(R.id.defaultReminderTimeButton)
        sahkoNightWindowText = findViewById(R.id.sahkoNightWindowText)
        sahkoNightWindowButton = findViewById(R.id.sahkoNightWindowButton)
        handsFreeVoiceStatusText = findViewById(R.id.handsFreeVoiceStatusText)
        handsFreeVoiceSwitch = findViewById(R.id.handsFreeVoiceSwitch)
        nightSilentModeStatusText = findViewById(R.id.nightSilentModeStatusText)
        nightSilentModeSwitch = findViewById(R.id.nightSilentModeSwitch)
        emojiCategoriesStatusText = findViewById(R.id.emojiCategoriesStatusText)
        emojiCategoriesSwitch = findViewById(R.id.emojiCategoriesSwitch)
        tireFreezeThresholdText = findViewById(R.id.tireFreezeThresholdText)
        tireFreezeThresholdButton = findViewById(R.id.tireFreezeThresholdButton)
        tirePolicyText = findViewById(R.id.tirePolicyText)
        tirePolicyButton = findViewById(R.id.tirePolicyButton)
        previewRouteButton = findViewById(R.id.previewRouteButton)
        val busTimeButton: View = findViewById(R.id.busTimeButton)
        busTimeButton.setOnClickListener { showBusDialog() }
        busRouteText = findViewById(R.id.busRouteText)
        busStopText = findViewById(R.id.busStopText)
        busTimeText = findViewById(R.id.busTimeText)
        busMinutesText = findViewById(R.id.busMinutesText)
        busRealtimeText = findViewById(R.id.busRealtimeText)
        busSourceText = findViewById(R.id.busSourceText)
        busProgress = findViewById(R.id.busProgress)
        editPendingButton = findViewById(R.id.editPendingButton)
        clearDraftButton = findViewById(R.id.clearDraftButton)
        medicationExportLogButton = findViewById(R.id.medicationExportLogButton)
        medicationOpenLogButton = findViewById(R.id.medicationOpenLogButton)
        pendingActionsRow = findViewById(R.id.pendingActionsRow)
        val permissionsMicRow: View = findViewById(R.id.permissionsMicStatusRow)
        val permissionsCalendarRow: View = findViewById(R.id.permissionsCalendarStatusRow)
        val weatherFallbackRow: View = findViewById(R.id.weatherFallbackRow)
        val defaultReminderTimeRow: View = findViewById(R.id.defaultReminderTimeRow)
        val sahkoNightWindowRow: View = findViewById(R.id.sahkoNightWindowRow)
        val handsFreeVoiceRow: View = findViewById(R.id.handsFreeVoiceRow)
        val nightSilentModeRow: View = findViewById(R.id.nightSilentModeRow)
        val emojiCategoriesRow: View = findViewById(R.id.emojiCategoriesRow)
        val tireFreezeThresholdRow: View = findViewById(R.id.tireFreezeThresholdRow)
        val tirePolicyRow: View = findViewById(R.id.tirePolicyRow)

        findViewById<Button>(R.id.voiceButton).setOnClickListener {
            if (maybeShowDeferredPermissionReminderForVoice()) {
                return@setOnClickListener
            }
            startVoiceInput()
        }

        findViewById<Button>(R.id.photoButton).setOnClickListener {
            showPhotoSourceDialog()
        }

        findViewById<Button>(R.id.previewButton).setOnClickListener {
            updatePreview(reminderInput.text?.toString().orEmpty())
        }

        previewRouteButton.setOnClickListener {
            val parsed = pendingReminder ?: parseReminderWithDomainFallback(reminderInput.text?.toString().orEmpty())
            val destination = parsed?.location.orEmpty().trim()
            if (destination.isBlank()) {
                Toast.makeText(this, getString(R.string.route_location_missing), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            openDirections(destination, parsed!!.eventDateTime)
        }

        reminderInput.doAfterTextChanged {
            saveDraftToPrefs()
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            if (maybeShowDeferredPermissionReminderForCalendar()) {
                return@setOnClickListener
            }

            val text = reminderInput.text?.toString().orEmpty()

            if (handleSensitiveDataCommand(text)) {
                return@setOnClickListener
            }

            if (handleSahkoVahtiCommand(text)) {
                return@setOnClickListener
            }

            if (handleSaunaTimerCommand(text)) {
                return@setOnClickListener
            }

            if (handleParkingControlCommand(text)) {
                return@setOnClickListener
            }

            if (handleParcelPickupCommand(text)) {
                return@setOnClickListener
            }

            if (handleWastePickupCommand(text)) {
                return@setOnClickListener
            }

            if (handleChildCareCommand(text)) {
                return@setOnClickListener
            }

            if (handleExpenseLogCommand(text)) {
                return@setOnClickListener
            }

            val parsed = parseReminderWithDomainFallback(text)

            if (parsed == null) {
                pendingReminder = null
                updatePendingEditVisibility()
                saveDraftToPrefs()
                parsedPreview.text = getString(R.string.parse_failed_hint)
                Toast.makeText(this, getString(R.string.parse_failed_toast), Toast.LENGTH_LONG).show()
                openSystemEventForm(text)
                return@setOnClickListener
            }

            showConfirmationDialog(parsed)
        }

        editPendingButton.setOnClickListener {
            val reminder = pendingReminder ?: return@setOnClickListener
            showConfirmationDialog(reminder)
        }

        clearDraftButton.setOnClickListener {
            showClearDraftConfirmDialog()
        }

        medicationExportLogButton.setOnClickListener {
            exportMedicationLog()
        }

        medicationOpenLogButton.setOnClickListener {
            markDrawerSectionActive(weatherFallbackRow)
            openMedicationLogDialog()
        }

        weatherFallbackCityButton.setOnClickListener {
            markDrawerSectionActive(weatherFallbackRow)
            showWeatherFallbackCityDialog()
        }

        defaultReminderTimeButton.setOnClickListener {
            markDrawerSectionActive(defaultReminderTimeRow)
            showDefaultReminderTimeDialog()
        }

        sahkoNightWindowButton.setOnClickListener {
            markDrawerSectionActive(sahkoNightWindowRow)
            showSahkoNightWindowDialog()
        }

        handsFreeVoiceSwitch.isChecked = isHandsFreeVoiceConfirmationEnabled()
        updateHandsFreeVoiceStatusUi(handsFreeVoiceSwitch.isChecked)
        handsFreeVoiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            markDrawerSectionActive(handsFreeVoiceRow)
            setHandsFreeVoiceConfirmationEnabled(isChecked)
            updateHandsFreeVoiceStatusUi(isChecked)
        }

        nightSilentModeSwitch.isChecked = isNightSilentModeEnabled()
        updateNightSilentModeStatusUi(nightSilentModeSwitch.isChecked)
        nightSilentModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            markDrawerSectionActive(nightSilentModeRow)
            setNightSilentModeEnabled(isChecked)
            updateNightSilentModeStatusUi(isChecked)
        }

        emojiCategoriesSwitch.isChecked = isEmojiCategoriesEnabled()
        updateEmojiCategoriesStatusUi(emojiCategoriesSwitch.isChecked)
        emojiCategoriesSwitch.setOnCheckedChangeListener { _, isChecked ->
            markDrawerSectionActive(emojiCategoriesRow)
            setEmojiCategoriesEnabled(isChecked)
            updateEmojiCategoriesStatusUi(isChecked)
            val currentText = reminderInput.text?.toString().orEmpty()
            if (currentText.isNotBlank()) {
                updatePreview(currentText)
            }
        }

        tireFreezeThresholdButton.setOnClickListener {
            markDrawerSectionActive(tireFreezeThresholdRow)
            showTireFreezeThresholdDialog()
        }

        tirePolicyButton.setOnClickListener {
            markDrawerSectionActive(tirePolicyRow)
            showTirePolicyDatesDialog()
        }

        permissionsMicInfoButton.setOnClickListener {
            showPermissionPurposeDialog(
                titleRes = R.string.permissions_mic_info_title,
                messageRes = R.string.permissions_mic_info_message
            )
        }

        permissionsMicStatusRow.setOnClickListener {
            markDrawerSectionActive(permissionsMicRow)
            showPermissionPurposeDialog(
                titleRes = R.string.permissions_mic_info_title,
                messageRes = R.string.permissions_mic_info_message
            )
        }
        permissionsMicStatusRow.setOnLongClickListener {
            requestMicrophonePermissionFromStatus()
            true
        }

        permissionsCalendarInfoButton.setOnClickListener {
            showPermissionPurposeDialog(
                titleRes = R.string.permissions_calendar_info_title,
                messageRes = R.string.permissions_calendar_info_message
            )
        }

        permissionsCalendarStatusRow.setOnClickListener {
            markDrawerSectionActive(permissionsCalendarRow)
            showPermissionPurposeDialog(
                titleRes = R.string.permissions_calendar_info_title,
                messageRes = R.string.permissions_calendar_info_message
            )
        }
        permissionsCalendarStatusRow.setOnLongClickListener {
            requestCalendarPermissionsFromStatus()
            true
        }

        restoreState(savedInstanceState)
        if (savedInstanceState == null) {
            if (restoreDraftFromPrefs()) {
                showDraftRestoredSnackbar()
            }
            requestPermissionsOnFirstLaunchIfNeeded()
        }

        updatePermissionsStatusUi()
        updateWeatherFallbackCityUi()
        updateDefaultReminderTimeUi()
        updateSahkoNightWindowUi()
        updateTireSettingsUi()
        ExpenseWeeklyReportScheduler.ensureScheduled(this)
        markDrawerSectionActive(weatherFallbackRow)
        handleLaunchIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        saveDraftToPrefs()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        busAnnouncer?.shutdown()
        busAnnouncer = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionsStatusUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_INPUT_TEXT, reminderInput.text?.toString().orEmpty())

        val reminder = pendingReminder ?: return
        outState.putString(STATE_PENDING_TITLE, reminder.title)
        outState.putString(STATE_PENDING_DATETIME, reminder.eventDateTime.toString())
        outState.putBoolean(STATE_PENDING_USED_DEFAULT_TIME, reminder.usedDefaultTime)
        outState.putString(STATE_PENDING_LOCATION, reminder.location)
        outState.putInt(STATE_PENDING_DURATION, reminder.durationMinutes)
    }

    private fun updatePreview(text: String) {
        val sahkoCommand = SahkoVahtiCommandParser.extract(text)
        if (sahkoCommand != null) {
            parsedPreview.text = getString(R.string.sahko_preview_template, sahkoCommand.applianceLabel)
            previewRouteButton.isEnabled = false
            return
        }

        val saunaCommand = SaunaTimerCommandParser.extract(text)
        if (saunaCommand != null) {
            parsedPreview.text = getString(R.string.sauna_timer_preview_template, saunaCommand.minutes)
            previewRouteButton.isEnabled = false
            return
        }

        val parkingCommand = ParkingControlCommandParser.extract(text)
        if (parkingCommand != null) {
            parsedPreview.text = getString(
                R.string.parking_preview_template,
                parkingCommand.expiresAt.toLocalTime().format(timeFormatter)
            )
            previewRouteButton.isEnabled = false
            return
        }

        val parcelCommand = ParcelPickupCommandParser.extract(text)
        if (parcelCommand != null) {
            parsedPreview.text = getString(
                R.string.parcel_preview_template,
                parcelCommand.itemLabel,
                parcelCommand.pickupPlace,
                parcelCommand.deadlineAt.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault()))
            )
            previewRouteButton.isEnabled = false
            return
        }

        val wasteCommand = WastePickupCommandParser.extract(text)
        if (wasteCommand != null) {
            parsedPreview.text = getString(
                R.string.waste_preview_template,
                wasteCommand.materialLabel,
                wasteCommand.dayOfWeek.name,
                wasteCommand.intervalWeeks
            )
            previewRouteButton.isEnabled = false
            return
        }

        val expenseCommand = ExpenseLogCommandParser.extract(text)
        if (expenseCommand != null) {
            parsedPreview.text = getString(
                R.string.expense_preview_template,
                String.format(Locale.US, "%.2f", expenseCommand.amountEuro),
                expenseCommand.note
            )
            previewRouteButton.isEnabled = false
            return
        }

        val childCommand = ChildCareCommandParser.extract(text)
        if (childCommand != null) {
            parsedPreview.text = when (childCommand) {
                is ChildCareCommand.LogEvent -> {
                    getString(
                        R.string.child_care_preview_log,
                        childCommand.note,
                        childCommand.happenedAt.toLocalTime().format(timeFormatter)
                    )
                }

                is ChildCareCommand.QueryLastEvent -> {
                    getString(
                        R.string.child_care_preview_query,
                        childEventTypeLabel(childCommand.eventType)
                    )
                }
            }
            previewRouteButton.isEnabled = false
            return
        }

        val parsed = parseReminderWithDomainFallback(text)
        if (parsed == null) {
            parsedPreview.text = getString(R.string.parse_failed_hint)
            previewRouteButton.isEnabled = false
            return
        }

        parsedPreview.text = getString(
            R.string.preview_template,
            parsed.title,
            parsed.eventDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())),
            if (parsed.usedDefaultTime) {
                getString(R.string.default_time_note, parsed.eventDateTime.toLocalTime().format(timeFormatter))
            } else {
                ""
            },
            parsed.location ?: getString(R.string.location_not_specified),
            getString(R.string.duration_minutes_template, parsed.durationMinutes)
        )
        previewRouteButton.isEnabled = !parsed.location.isNullOrBlank()
    }

    private fun ensureCalendarPermissionAndSave() {
        val hasRead = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val hasWrite = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (hasRead && hasWrite) {
            savePendingReminder()
            return
        }

        requestCalendarPermissions.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )
    }

    private fun savePendingReminder() {
        val reminder = pendingReminder ?: return
        val calendarId = getWritableCalendarId()
        val medicationPlan = extractMedicationPlan(reminder)

        if (calendarId == null) {
            Toast.makeText(this, getString(R.string.calendar_not_found), Toast.LENGTH_LONG).show()
            return
        }

        val startMillis = reminder.eventDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val endMillis = reminder.eventDateTime
            .plusMinutes(reminder.durationMinutes.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, reminder.title)
            put(CalendarContract.Events.DESCRIPTION, reminder.title)
            reminder.location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            medicationPlan?.let {
                put(CalendarContract.Events.RRULE, MedicationReminderParser.toDailyRRule(it.daysCount))
            }
        }

        val createdUri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (createdUri != null) {
            val eventId = createdUri.lastPathSegment ?: "${reminder.title}_${startMillis}"
            createdUri.lastPathSegment?.toLongOrNull()?.let { eventIdLong ->
                addLibraryReturnRemindersIfNeeded(eventIdLong, reminder)
                maybeRegisterStoreGeoReminder(eventIdLong, reminder)
            }
            maybeWarnEarlyFrostForTires(reminder)

            MedicationReminderNotifier.ensureChannel(this)
            scheduleMedicationReminderIfNeeded(
                eventId = eventId,
                reminder = reminder,
                triggerEpochMillis = startMillis,
                medicationPlan = medicationPlan
            )

            MessageReminderNotifier.ensureChannel(this)
            scheduleMessageReminderIfNeeded(
                reminder = reminder,
                eventId = eventId,
                triggerEpochMillis = startMillis
            )

            WeatherAlertNotifier.ensureChannel(this)
            WeatherAlertScheduler.schedule(
                context = this,
                eventId = eventId,
                eventTitle = reminder.title,
                eventLocation = reminder.location.orEmpty(),
                fallbackLocation = getWeatherFallbackCity(),
                eventEpochMillis = startMillis,
                timezoneId = ZoneId.systemDefault().id
            )

            val message = if (reminder.usedDefaultTime) {
                getString(R.string.saved_with_default_time, reminder.eventDateTime.toLocalTime().format(timeFormatter))
            } else {
                getString(R.string.saved_successfully)
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            showDirectionsAction(reminder)
            parsedPreview.text = getString(R.string.saved_event_preview, reminder.title)
            reminderInput.text?.clear()
            pendingReminder = null
            updatePendingEditVisibility()
            clearDraftFromPrefs()
        } else {
            Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_LONG).show()
        }
    }

    private fun scheduleMessageReminderIfNeeded(
        reminder: ParsedReminder,
        eventId: String,
        triggerEpochMillis: Long
    ) {
        val rawInput = reminderInput.text?.toString().orEmpty()
        val command = MessageReminderParser.extract(rawInput) ?: return
        val recipient = command.recipient?.ifBlank { reminder.location }

        MessageReminderScheduler.schedule(
            context = this,
            reminderId = eventId,
            triggerEpochMillis = triggerEpochMillis,
            recipient = recipient,
            messageText = command.messageText,
            preferredChannel = command.preferredChannel.name
        )
    }

    private fun extractMedicationPlan(reminder: ParsedReminder): com.proapps.voiceremind.medication.MedicationPlan? {
        val rawInput = reminderInput.text?.toString().orEmpty()
        val source = rawInput.ifBlank { reminder.title }
        return MedicationReminderParser.extract(
            rawText = source,
            now = LocalDateTime.now(),
            defaultTime = getConfiguredDefaultReminderTime()
        )
    }

    private fun scheduleMedicationReminderIfNeeded(
        eventId: String,
        reminder: ParsedReminder,
        triggerEpochMillis: Long,
        medicationPlan: com.proapps.voiceremind.medication.MedicationPlan?
    ) {
        val plan = medicationPlan ?: return
        MedicationReminderScheduler.schedule(
            context = this,
            planId = eventId,
            title = reminder.title,
            firstTriggerEpochMillis = triggerEpochMillis,
            daysCount = plan.daysCount
        )
    }

    private fun applyDomainDefaults(reminder: ParsedReminder): ParsedReminder {
        val withCategory = if (isEmojiCategoriesEnabled()) {
            reminder.copy(title = ReminderCategoryEmoji.apply(reminder.title))
        } else {
            reminder
        }

        if (!isLibraryReturnReminder(withCategory) || !withCategory.usedDefaultTime) {
            return withCategory
        }

        val closingTime = LocalTime.of(LIBRARY_CLOSING_HOUR, 0)
        return withCategory.copy(
            eventDateTime = LocalDateTime.of(withCategory.eventDateTime.toLocalDate(), closingTime)
        )
    }

    private fun parseReminderWithDomainFallback(rawText: String): ParsedReminder? {
        val parsed = parser.parse(rawText)
        if (parsed != null) {
            return applyDomainDefaults(parsed)
        }

        MessageReminderParser.extract(rawText)?.let { command ->
            val triggerDateTime = command.resolveTriggerDateTime(
                now = LocalDateTime.now(),
                defaultTime = getConfiguredDefaultReminderTime()
            )

            val title = if (command.recipient.isNullOrBlank()) {
                getString(R.string.message_reminder_title_generic)
            } else {
                getString(R.string.message_reminder_title_with_recipient, command.recipient)
            }

            return ParsedReminder(
                title = title,
                eventDateTime = triggerDateTime,
                usedDefaultTime = command.explicitTime == null,
                location = command.recipient,
                durationMinutes = 15
            )
        }

        MedicationReminderParser.extract(
            rawText = rawText,
            now = LocalDateTime.now(),
            defaultTime = getConfiguredDefaultReminderTime()
        )?.let { plan ->
            return ParsedReminder(
                title = plan.title,
                eventDateTime = plan.firstIntakeDateTime,
                usedDefaultTime = plan.usedDefaultTime,
                location = null,
                durationMinutes = 10
            )
        }

        if (!TireChangePolicy.isTireChangeRequest(rawText)) {
            return null
        }

        val policy = TireChangePolicy.resolveOfficialDate(
            now = LocalDate.now(),
            requestedSeason = TireChangePolicy.detectSeason(rawText),
            summerMonth = getTireSummerMonth(),
            summerDay = getTireSummerDay(),
            winterMonth = getTireWinterMonth(),
            winterDay = getTireWinterDay()
        )
        val title = rawText.trim().ifBlank { getString(R.string.tire_default_title) }
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        return ParsedReminder(
            title = title,
            eventDateTime = LocalDateTime.of(policy.officialDate, getConfiguredDefaultReminderTime()),
            usedDefaultTime = true,
            location = null,
            durationMinutes = 60
        )
    }

    private fun maybeWarnEarlyFrostForTires(reminder: ParsedReminder) {
        if (!TireChangePolicy.isTireChangeRequest(reminder.title)) {
            return
        }

        val policy = TireChangePolicy.resolveOfficialDate(
            now = LocalDate.now(),
            requestedSeason = TireChangePolicy.detectSeason(reminder.title),
            summerMonth = getTireSummerMonth(),
            summerDay = getTireSummerDay(),
            winterMonth = getTireWinterMonth(),
            winterDay = getTireWinterDay()
        )
        val location = reminder.location?.trim().orEmpty().ifBlank { getWeatherFallbackCity() }
        val timezoneId = ZoneId.systemDefault().id

        Thread {
            val client = OpenMeteoClient()
            val point = client.geocode(location) ?: return@Thread
            val forecast = client.loadTemperatureForecast(
                latitude = point.latitude,
                longitude = point.longitude,
                timezoneId = timezoneId,
                forecastDays = TIRE_FROST_CHECK_DAYS
            ) ?: return@Thread

            val hasEarlyFreeze = TireFreezeAdvisor.hasEarlyFreezeBeforeOfficialDate(
                officialDate = policy.officialDate,
                timezoneId = timezoneId,
                hourlyEpochSeconds = forecast.hourlyEpochSeconds,
                temperatureC = forecast.temperatureC,
                freezeThresholdC = getTireFreezeThresholdC()
            )

            if (!hasEarlyFreeze) return@Thread

            val suggestedDate = policy.officialDate.minusWeeks(1)
            val warning = getString(
                R.string.tire_early_frost_warning,
                suggestedDate.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault()))
            )

            runOnUiThread {
                Snackbar.make(findViewById(R.id.main), warning, Snackbar.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun maybeRegisterStoreGeoReminder(eventId: Long, reminder: ParsedReminder) {
        if (!isStoreRelatedReminder(reminder)) return

        val placeName = resolveStorePlaceName(reminder) ?: return
        val request = StoreGeoRequest(eventId = eventId, placeName = placeName)

        if (!StoreGeofenceManager.hasRequiredLocationPermission(this)) {
            pendingStoreGeoRequest = request
            requestStoreGeoPermissions.launch(requiredStoreGeoPermissions())
            return
        }

        registerStoreGeoReminder(request)
    }

    private fun registerStoreGeoReminder(request: StoreGeoRequest) {
        Thread {
            val client = OpenMeteoClient()
            val point = client.geocode(request.placeName)

            runOnUiThread {
                if (point == null) {
                    Toast.makeText(this, getString(R.string.store_geo_geocode_failed), Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                runCatching {
                    StoreGeofenceManager.registerStoreEnterGeofence(
                        context = this,
                        requestId = "store_geo_${request.eventId}",
                        latitude = point.latitude,
                        longitude = point.longitude,
                        placeName = request.placeName
                    )
                }.onSuccess {
                    Toast.makeText(this, getString(R.string.store_geo_armed, request.placeName), Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(this, getString(R.string.store_geo_register_failed), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun requiredStoreGeoPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun isStoreRelatedReminder(reminder: ParsedReminder): Boolean {
        return StoreGeoReminderMatcher.isStoreRelated(
            title = reminder.title,
            location = reminder.location
        )
    }

    private fun resolveStorePlaceName(reminder: ParsedReminder): String? {
        return StoreGeoReminderMatcher.resolvePlaceName(
            title = reminder.title,
            location = reminder.location,
            fallbackCity = getWeatherFallbackCity()
        )
    }

    private fun isLibraryReturnReminder(reminder: ParsedReminder): Boolean {
        val source = "${reminder.title} ${reminder.location.orEmpty()}".lowercase(Locale.ROOT)
        val libraryKeywords = listOf("библиот", "library", "kirjasto", "oodi", "helmet")
        val returnKeywords = listOf("верну", "вернуть", "return", "palaut", "kirja")
        return libraryKeywords.any { source.contains(it) } && returnKeywords.any { source.contains(it) }
    }

    private fun addLibraryReturnRemindersIfNeeded(eventId: Long, reminder: ParsedReminder) {
        if (!isLibraryReturnReminder(reminder)) {
            return
        }

        insertCalendarReminder(eventId, LIBRARY_REMINDER_TWO_DAYS_MINUTES)
        insertCalendarReminder(eventId, LIBRARY_REMINDER_ONE_HOUR_MINUTES)
    }

    private fun insertCalendarReminder(eventId: Long, minutesBefore: Int) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, minutesBefore)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
    }

    private fun getWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.VISIBLE
        )

        val selection = "${CalendarContract.Calendars.VISIBLE}=1"
        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        return null
    }

    private fun startVoiceInput() {
        isAwaitingVoiceDecision = false
        pendingVoiceReminder = null

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
            }
        }

        if (intent.resolveActivity(packageManager) != null) {
            speechLauncher.launch(intent)
        } else {
            Toast.makeText(this, getString(R.string.voice_not_supported), Toast.LENGTH_LONG).show()
        }
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.action != ACTION_START_VOICE_INPUT) {
            return
        }
        intent.action = null

        reminderInput.post {
            if (maybeShowDeferredPermissionReminderForVoice()) {
                return@post
            }
            startVoiceInput()
        }
    }

    private fun startPhotoCapture() {
        takePhotoLauncher.launch(null)
    }

    private fun startPhotoPickFromGallery() {
        pickPhotoLauncher.launch("image/*")
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf(
            getString(R.string.photo_source_camera),
            getString(R.string.photo_source_gallery)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.photo_source_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startPhotoCapture()
                    1 -> startPhotoPickFromGallery()
                }
            }
            .show()
    }

    private fun processAnnouncementPhoto(bitmap: Bitmap) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                handleOcrRecognizedText(result.text)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.ocr_failed), Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                recognizer.close()
            }
    }

    private fun processAnnouncementPhotoFromUri(uri: Uri) {
        val image = runCatching { InputImage.fromFilePath(this, uri) }.getOrNull()
        if (image == null) {
            Toast.makeText(this, getString(R.string.ocr_failed), Toast.LENGTH_LONG).show()
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                handleOcrRecognizedText(result.text)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.ocr_failed), Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                recognizer.close()
            }
    }

    private fun handleOcrRecognizedText(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) {
            Toast.makeText(this, getString(R.string.ocr_no_text), Toast.LENGTH_LONG).show()
            return
        }

        reminderInput.setText(text)
        updatePreview(text)

        val parsed = parseReminderWithDomainFallback(text)
        if (parsed == null) {
            Toast.makeText(this, getString(R.string.ocr_parse_failed), Toast.LENGTH_LONG).show()
            return
        }

        showConfirmationDialog(parsed)
    }

    private fun startVoiceDecisionInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_confirmation_listen_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
            }
        }

        if (intent.resolveActivity(packageManager) != null) {
            speechLauncher.launch(intent)
        } else {
            isAwaitingVoiceDecision = false
            Toast.makeText(this, getString(R.string.voice_not_supported), Toast.LENGTH_LONG).show()
        }
    }

    private fun askVoiceConfirmation(parsed: ParsedReminder) {
        pendingVoiceReminder = parsed
        isAwaitingVoiceDecision = true

        val question = getString(
            R.string.voice_confirmation_question,
            parsed.title,
            parsed.eventDateTime.toLocalTime().format(timeFormatter)
        )

        if (isNightSilentModeEnabled() && isQuietHoursNow()) {
            vibrateQuietFeedback()
            Toast.makeText(this, getString(R.string.night_mode_silent_confirmation), Toast.LENGTH_LONG).show()
            startVoiceDecisionInput()
            return
        }

        if (isTtsReady) {
            tts?.speak(question, TextToSpeech.QUEUE_FLUSH, null, VOICE_CONFIRM_UTTERANCE_ID)
        } else {
            Toast.makeText(this, question, Toast.LENGTH_LONG).show()
            startVoiceDecisionInput()
        }
    }

    private fun isQuietHoursNow(): Boolean {
        val nowHour = LocalTime.now().hour
        return nowHour >= QUIET_HOURS_START || nowHour < QUIET_HOURS_END
    }

    private fun isNightSilentModeEnabled(): Boolean {
        return getSettingsPrefs().getBoolean(NIGHT_SILENT_MODE_ENABLED, true)
    }

    private fun isEmojiCategoriesEnabled(): Boolean {
        return getSettingsPrefs().getBoolean(EMOJI_CATEGORIES_ENABLED, true)
    }

    private fun setNightSilentModeEnabled(enabled: Boolean) {
        getSettingsPrefs()
            .edit()
            .putBoolean(NIGHT_SILENT_MODE_ENABLED, enabled)
            .apply()
    }

    private fun setEmojiCategoriesEnabled(enabled: Boolean) {
        getSettingsPrefs()
            .edit()
            .putBoolean(EMOJI_CATEGORIES_ENABLED, enabled)
            .apply()
    }

    private fun updateEmojiCategoriesStatusUi(isEnabled: Boolean) {
        applyToggleStatusUi(
            statusTextView = emojiCategoriesStatusText,
            isEnabled = isEnabled,
            onTextRes = R.string.emoji_categories_status_on,
            offTextRes = R.string.emoji_categories_status_off,
            animate = false
        )
    }

    private fun updateNightSilentModeStatusUi(isEnabled: Boolean) {
        applyToggleStatusUi(
            statusTextView = nightSilentModeStatusText,
            isEnabled = isEnabled,
            onTextRes = R.string.night_mode_status_on,
            offTextRes = R.string.night_mode_status_off,
            animate = false
        )
    }

    private fun vibrateQuietFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java) ?: return
            manager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(QUIET_FEEDBACK_VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            )
            return
        }

        @Suppress("DEPRECATION")
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        @Suppress("DEPRECATION")
        vibrator.vibrate(QUIET_FEEDBACK_VIBRATION_MS)
    }

    private fun handleVoiceDecision(rawDecision: String) {
        val decision = rawDecision.trim().lowercase(Locale.ROOT)

        when {
            isVoiceYes(decision) -> {
                isAwaitingVoiceDecision = false
                val reminder = pendingVoiceReminder
                pendingVoiceReminder = null

                if (reminder == null) {
                    Toast.makeText(this, getString(R.string.voice_confirmation_failed), Toast.LENGTH_LONG).show()
                    return
                }

                pendingReminder = reminder
                updatePendingEditVisibility()
                saveDraftToPrefs()
                ensureCalendarPermissionAndSave()
            }

            isVoiceNo(decision) -> {
                isAwaitingVoiceDecision = false
                pendingVoiceReminder = null
                Toast.makeText(this, getString(R.string.voice_confirmation_cancelled), Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(this, getString(R.string.voice_confirmation_retry), Toast.LENGTH_SHORT).show()
                startVoiceDecisionInput()
            }
        }
    }

    private fun isVoiceYes(text: String): Boolean {
        val yesWords = setOf("да", "yes", "yep", "yeah", "ok", "okay", "ага", "так", "kylla", "joo")
        return yesWords.any { word -> text == word || text.startsWith("$word ") }
    }

    private fun isVoiceNo(text: String): Boolean {
        val noWords = setOf("нет", "no", "nope", "ні", "ni", "ei")
        return noWords.any { word -> text == word || text.startsWith("$word ") }
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                isTtsReady = false
                return@TextToSpeech
            }

            val result = tts?.setLanguage(Locale.getDefault())
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != VOICE_CONFIRM_UTTERANCE_ID || !isAwaitingVoiceDecision) return
                    runOnUiThread {
                        if (isAwaitingVoiceDecision) {
                            startVoiceDecisionInput()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (utteranceId != VOICE_CONFIRM_UTTERANCE_ID || !isAwaitingVoiceDecision) return
                    runOnUiThread {
                        if (isAwaitingVoiceDecision) {
                            startVoiceDecisionInput()
                        }
                    }
                }
            })
        }
    }

    private fun openSystemEventForm(rawText: String) {
        val eventTitle = rawText.ifBlank { getString(R.string.default_event_title) }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, eventTitle)
            putExtra(CalendarContract.Events.DESCRIPTION, rawText)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            Toast.makeText(this, getString(R.string.calendar_form_opened), Toast.LENGTH_LONG).show()
        }
    }

    private fun showConfirmationDialog(parsed: ParsedReminder) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_reminder, null)

        val titleLayout = view.findViewById<TextInputLayout>(R.id.confirmTitleLayout)
        val dateLayout = view.findViewById<TextInputLayout>(R.id.confirmDateLayout)
        val timeLayout = view.findViewById<TextInputLayout>(R.id.confirmTimeLayout)
        val durationLayout = view.findViewById<TextInputLayout>(R.id.confirmDurationLayout)
        val locationLayout = view.findViewById<TextInputLayout>(R.id.confirmLocationLayout)

        val titleInput = view.findViewById<TextInputEditText>(R.id.confirmTitleInput)
        val dateInput = view.findViewById<TextInputEditText>(R.id.confirmDateInput)
        val timeInput = view.findViewById<TextInputEditText>(R.id.confirmTimeInput)
        val durationInput = view.findViewById<TextInputEditText>(R.id.confirmDurationInput)
        val locationInput = view.findViewById<TextInputEditText>(R.id.confirmLocationInput)

        var selectedDate = parsed.eventDateTime.toLocalDate()
        var selectedTime = parsed.eventDateTime.toLocalTime().withSecond(0).withNano(0)

        titleInput.setText(parsed.title)
        dateInput.setText(selectedDate.format(dateFormatter))
        timeInput.setText(selectedTime.format(timeFormatter))
        durationInput.setText(parsed.durationMinutes.toString())
        locationInput.setText(parsed.location.orEmpty())

        dateInput.setOnClickListener {
            openDatePicker(selectedDate) { pickedDate ->
                selectedDate = pickedDate
                dateInput.setText(selectedDate.format(dateFormatter))
                dateLayout.error = null
            }
        }

        timeInput.setOnClickListener {
            openTimePicker(selectedTime) { pickedTime ->
                selectedTime = pickedTime
                timeInput.setText(selectedTime.format(timeFormatter))
                timeLayout.error = null
            }
        }

        dateLayout.setEndIconOnClickListener {
            dateInput.performClick()
        }

        timeLayout.setEndIconOnClickListener {
            timeInput.performClick()
        }

        durationInput.doAfterTextChanged {
            durationLayout.error = null
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_dialog_title)
            .setView(view)
            .setNegativeButton(R.string.confirm_dialog_cancel, null)
            .setPositiveButton(R.string.confirm_dialog_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                titleLayout.error = null
                dateLayout.error = null
                timeLayout.error = null
                durationLayout.error = null
                locationLayout.error = null

                val title = titleInput.text?.toString()?.trim().orEmpty().ifBlank {
                    getString(R.string.default_event_title)
                }

                val duration = durationInput.text?.toString()?.trim()?.toIntOrNull()
                val location = locationInput.text?.toString()?.trim().orEmpty().ifBlank { null }

                var hasError = false
                if (duration == null || duration !in 1..1440) {
                    durationLayout.error = getString(R.string.confirm_duration_error)
                    hasError = true
                }

                if (hasError) {
                    return@setOnClickListener
                }

                pendingReminder = parsed.copy(
                    title = title,
                    eventDateTime = LocalDateTime.of(selectedDate, selectedTime),
                    usedDefaultTime = false,
                    location = location,
                    durationMinutes = duration!!
                )

                dialog.dismiss()
                updatePendingEditVisibility()
                saveDraftToPrefs()
                showSaveSnackbar()
            }
        }

        dialog.show()
    }

    private fun openDatePicker(initialDate: LocalDate, onPicked: (LocalDate) -> Unit) {
        val todayStartMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val initialSelection = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.from(todayStartMillis))
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraints)
            .setSelection(initialSelection)
            .build()

        picker.addOnPositiveButtonClickListener { selectedMillis ->
            val date = Instant.ofEpochMilli(selectedMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            onPicked(date)
        }

        picker.show(supportFragmentManager, "confirm_date_picker")
    }

    private fun openDatePickerUnrestricted(
        initialDate: LocalDate,
        tag: String,
        onPicked: (LocalDate) -> Unit
    ) {
        val initialSelection = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(initialSelection)
            .build()

        picker.addOnPositiveButtonClickListener { selectedMillis ->
            val date = Instant.ofEpochMilli(selectedMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            onPicked(date)
        }

        picker.show(supportFragmentManager, tag)
    }

    private fun openTimePicker(initialTime: LocalTime, onPicked: (LocalTime) -> Unit) {
        val timeFormat = if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val picker = MaterialTimePicker.Builder()
            .setHour(initialTime.hour)
            .setMinute(initialTime.minute)
            .setTimeFormat(timeFormat)
            .build()

        picker.addOnPositiveButtonClickListener {
            onPicked(LocalTime.of(picker.hour, picker.minute))
        }

        picker.show(supportFragmentManager, "confirm_time_picker")
    }

    private fun showSaveSnackbar() {
        val reminder = pendingReminder ?: return
        val summary = getString(
            R.string.save_snackbar_summary,
            reminder.eventDateTime.format(dateFormatter),
            reminder.eventDateTime.format(timeFormatter),
            reminder.location ?: getString(R.string.location_not_specified),
            reminder.durationMinutes
        )

        Snackbar.make(findViewById(R.id.main), summary, Snackbar.LENGTH_LONG)
            .setAction(R.string.save_snackbar_action) {
                ensureCalendarPermissionAndSave()
            }
            .show()
    }

    private fun updatePendingEditVisibility() {
        pendingActionsRow.visibility = if (pendingReminder != null) View.VISIBLE else View.GONE
    }

    private fun restoreState(savedState: Bundle?) {
        if (savedState == null) {
            updatePendingEditVisibility()
            return
        }

        val restoredInput = savedState.getString(STATE_INPUT_TEXT).orEmpty()
        if (restoredInput.isNotBlank()) {
            reminderInput.setText(restoredInput)
            updatePreview(restoredInput)
        }

        val pendingDateTime = savedState.getString(STATE_PENDING_DATETIME)?.let {
            runCatching { LocalDateTime.parse(it) }.getOrNull()
        }
        val pendingTitle = savedState.getString(STATE_PENDING_TITLE)
        val pendingDuration = savedState.getInt(STATE_PENDING_DURATION, 0)

        pendingReminder = if (pendingDateTime != null && !pendingTitle.isNullOrBlank() && pendingDuration > 0) {
            ParsedReminder(
                title = pendingTitle,
                eventDateTime = pendingDateTime,
                usedDefaultTime = savedState.getBoolean(STATE_PENDING_USED_DEFAULT_TIME, false),
                location = savedState.getString(STATE_PENDING_LOCATION),
                durationMinutes = pendingDuration
            )
        } else {
            null
        }

        updatePendingEditVisibility()
    }

    private fun saveDraftToPrefs() {
        val prefs = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE)
        prefs.edit().apply {
            putString(DRAFT_INPUT_TEXT, reminderInput.text?.toString().orEmpty())

            val reminder = pendingReminder
            if (reminder == null) {
                remove(DRAFT_PENDING_TITLE)
                remove(DRAFT_PENDING_DATETIME)
                remove(DRAFT_PENDING_USED_DEFAULT_TIME)
                remove(DRAFT_PENDING_LOCATION)
                remove(DRAFT_PENDING_DURATION)
            } else {
                putString(DRAFT_PENDING_TITLE, reminder.title)
                putString(DRAFT_PENDING_DATETIME, reminder.eventDateTime.toString())
                putBoolean(DRAFT_PENDING_USED_DEFAULT_TIME, reminder.usedDefaultTime)
                putString(DRAFT_PENDING_LOCATION, reminder.location)
                putInt(DRAFT_PENDING_DURATION, reminder.durationMinutes)
            }
        }.apply()
    }

    private fun restoreDraftFromPrefs(): Boolean {
        var restoredAny = false
        val prefs = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE)

        val draftInput = prefs.getString(DRAFT_INPUT_TEXT, "").orEmpty()
        if (draftInput.isNotBlank() && reminderInput.text.isNullOrBlank()) {
            reminderInput.setText(draftInput)
            updatePreview(draftInput)
            restoredAny = true
        }

        val pendingDateTime = prefs.getString(DRAFT_PENDING_DATETIME, null)?.let {
            runCatching { LocalDateTime.parse(it) }.getOrNull()
        }
        val pendingTitle = prefs.getString(DRAFT_PENDING_TITLE, null)
        val pendingDuration = prefs.getInt(DRAFT_PENDING_DURATION, 0)

        if (pendingReminder == null && pendingDateTime != null && !pendingTitle.isNullOrBlank() && pendingDuration > 0) {
            pendingReminder = ParsedReminder(
                title = pendingTitle,
                eventDateTime = pendingDateTime,
                usedDefaultTime = prefs.getBoolean(DRAFT_PENDING_USED_DEFAULT_TIME, false),
                location = prefs.getString(DRAFT_PENDING_LOCATION, null),
                durationMinutes = pendingDuration
            )
            restoredAny = true
        }

        updatePendingEditVisibility()
        return restoredAny
    }

    private fun clearDraftFromPrefs() {
        getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun exportMedicationLog() {
        val shareIntent = MedicationLogShareHelper.buildShareIntent(this)
        if (shareIntent == null) {
            Toast.makeText(this, getString(R.string.medication_export_no_log), Toast.LENGTH_LONG).show()
            return
        }

        val chooser = Intent.createChooser(shareIntent, getString(R.string.medication_export_chooser_title))
        if (shareIntent.resolveActivity(packageManager) != null || chooser.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            Toast.makeText(this, getString(R.string.message_no_app_found), Toast.LENGTH_LONG).show()
        }
    }

    private fun openMedicationLogDialog() {
        val entries = MedicationLogStore.readLatestEntries(this, limit = 10)
        if (entries.isEmpty()) {
            Toast.makeText(this, getString(R.string.medication_export_no_log), Toast.LENGTH_LONG).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.medication_open_log_title)
            .setMessage(entries.joinToString(separator = "\n"))
            .setPositiveButton(R.string.permissions_info_dialog_ok, null)
            .show()
    }

    private fun markDrawerSectionActive(section: View) {
        activeDrawerSection?.isActivated = false
        section.isActivated = true
        activeDrawerSection = section
    }

    private fun handleSensitiveDataCommand(rawText: String): Boolean {
        val command = SensitiveDataCommandParser.parse(rawText) ?: return false

        when (command) {
            is SensitiveDataCommand.Save -> {
                val saved = SensitiveDataVault.save(
                    context = this,
                    type = command.type,
                    value = command.value
                )

                if (!saved) {
                    Toast.makeText(this, getString(R.string.sensitive_data_save_failed), Toast.LENGTH_LONG).show()
                    return true
                }

                val typeLabel = getString(command.type.labelRes)
                parsedPreview.text = getString(R.string.sensitive_data_saved_preview, typeLabel)
                Toast.makeText(this, getString(R.string.sensitive_data_saved, typeLabel), Toast.LENGTH_LONG).show()
                pendingReminder = null
                updatePendingEditVisibility()
                saveDraftToPrefs()
                return true
            }

            is SensitiveDataCommand.Query -> {
                val value = SensitiveDataVault.load(this, command.type)
                val typeLabel = getString(command.type.labelRes)

                if (value.isNullOrBlank()) {
                    Toast.makeText(this, getString(R.string.sensitive_data_not_found, typeLabel), Toast.LENGTH_LONG).show()
                    return true
                }

                revealSensitiveDataWithBiometric(typeLabel, value)
                return true
            }
        }
    }

    private fun revealSensitiveDataWithBiometric(typeLabel: String, value: String) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, getString(R.string.sensitive_data_biometric_unavailable), Toast.LENGTH_LONG).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(getString(R.string.sensitive_data_reveal_title, typeLabel))
                    .setMessage(value)
                    .setPositiveButton(R.string.permissions_info_dialog_ok, null)
                    .show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    return
                }
                Toast.makeText(this@MainActivity, getString(R.string.sensitive_data_biometric_failed), Toast.LENGTH_LONG).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.sensitive_data_biometric_title))
            .setSubtitle(getString(R.string.sensitive_data_biometric_subtitle, typeLabel))
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun handleSahkoVahtiCommand(rawText: String): Boolean {
        val command = SahkoVahtiCommandParser.extract(rawText) ?: return false

        SahkoVahtiScheduler.schedule(
            context = this,
            appliance = command.applianceLabel,
            nightHourStart = getSahkoNightStartHour(),
            nightHourEndExclusive = getSahkoNightEndHour()
        )

        Toast.makeText(
            this,
            getString(R.string.sahko_scheduled, command.applianceLabel),
            Toast.LENGTH_LONG
        ).show()
        parsedPreview.text = getString(R.string.sahko_preview_template, command.applianceLabel)
        reminderInput.text?.clear()
        return true
    }

    private fun handleSaunaTimerCommand(rawText: String): Boolean {
        val command = SaunaTimerCommandParser.extract(rawText) ?: return false

        SaunaTimerScheduler.schedule(
            context = this,
            minutes = command.minutes
        )

        Toast.makeText(
            this,
            getString(R.string.sauna_timer_scheduled, command.minutes),
            Toast.LENGTH_LONG
        ).show()
        parsedPreview.text = getString(R.string.sauna_timer_preview_template, command.minutes)
        reminderInput.text?.clear()
        return true
    }

    private fun handleParkingControlCommand(rawText: String): Boolean {
        val command = ParkingControlCommandParser.extract(rawText) ?: return false

        ParkingControlScheduler.schedule(
            context = this,
            expiresAt = command.expiresAt
        )

        val expiresAtText = command.expiresAt.toLocalTime().format(timeFormatter)
        Toast.makeText(
            this,
            getString(R.string.parking_scheduled, expiresAtText, command.remindBeforeMinutes),
            Toast.LENGTH_LONG
        ).show()
        parsedPreview.text = getString(R.string.parking_preview_template, expiresAtText)
        reminderInput.text?.clear()
        return true
    }

    private fun handleParcelPickupCommand(rawText: String): Boolean {
        val command = ParcelPickupCommandParser.extract(rawText) ?: return false
        val request = ParcelGeoRequest(
            itemLabel = command.itemLabel,
            pickupPlace = command.pickupPlace,
            deadlineAt = command.deadlineAt
        )

        if (!StoreGeofenceManager.hasRequiredLocationPermission(this)) {
            pendingParcelGeoRequest = request
            requestStoreGeoPermissions.launch(requiredStoreGeoPermissions())
            return true
        }

        registerParcelGeoReminder(request)
        return true
    }

    private fun registerParcelGeoReminder(request: ParcelGeoRequest) {
        Thread {
            val client = OpenMeteoClient()
            val point = client.geocode(request.pickupPlace)

            runOnUiThread {
                if (point == null) {
                    Toast.makeText(this, getString(R.string.parcel_geo_geocode_failed), Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                val deadlineEpoch = request.deadlineAt
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                runCatching {
                    ParcelPickupGeofenceManager.registerParcelEnterGeofence(
                        context = this,
                        requestId = "parcel_geo_${request.itemLabel.hashCode()}_${deadlineEpoch}",
                        latitude = point.latitude,
                        longitude = point.longitude,
                        itemLabel = request.itemLabel,
                        pickupPlace = request.pickupPlace,
                        deadlineEpochMillis = deadlineEpoch
                    )
                }.onSuccess {
                    val deadlineLabel = request.deadlineAt.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault()))
                    Toast.makeText(
                        this,
                        getString(R.string.parcel_geo_armed, request.itemLabel, request.pickupPlace, deadlineLabel),
                        Toast.LENGTH_LONG
                    ).show()
                    parsedPreview.text = getString(R.string.parcel_preview_template, request.itemLabel, request.pickupPlace, deadlineLabel)
                    reminderInput.text?.clear()
                }.onFailure {
                    Toast.makeText(this, getString(R.string.parcel_geo_register_failed), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun handleBusTimeQuery(rawText: String): Boolean {
        val route = parseRouteNumberFromText(rawText) ?: return false

        // we will try to obtain last known location; request permission if needed
        val hasLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // show quick UI feedback
        parsedPreview.text = "Ищу следующий автобус $route..."

        if (!hasLocation) {
            pendingBusRouteQuery = route
            // request location permission; reuse existing store geo permission flow
            requestStoreGeoPermissions.launch(requiredStoreGeoPermissions())
            Toast.makeText(this, "Требуется разрешение на местоположение для определения ближайшей остановки.", Toast.LENGTH_LONG).show()
            return true
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                    busProgress.visibility = View.VISIBLE
                    val router = selectRouterForLocation(loc.latitude, loc.longitude)
                    busAnnouncer?.announceNextBus(route, loc.latitude, loc.longitude, router = router) { departure ->
                        displayBusDeparture(departure, route)
                    }
                } else {
                    // fallback: geocode configured fallback city
                    parsedPreview.text = "Пытаюсь использовать город из настроек..."
                    Thread {
                        val client = OpenMeteoClient()
                        val fallback = getWeatherFallbackCity()
                        val point = client.geocode(fallback)
                        if (point != null) {
                            runOnUiThread { parsedPreview.text = "Использую $fallback для поиска" }
                            val router = selectRouterForLocation(point.latitude, point.longitude)
                            busProgress.visibility = View.VISIBLE
                            busAnnouncer?.announceNextBus(route, point.latitude, point.longitude, router = router) { departure ->
                                displayBusDeparture(departure, route)
                            }
                        } else {
                            val msg = "Не удалось определить координаты для $fallback."
                            runOnUiThread {
                                if (isTtsReady) tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "bus_query")
                                else Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }.start()
                }
            }
        } catch (e: Exception) {
            val msg = "Ошибка получения местоположения."
            if (isTtsReady) tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "bus_query")
        }

        return true
    }

    private fun showBusDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.bus_time_dialog_hint)
            setText(pendingBusRouteQuery ?: "")
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bus_time_dialog_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.bus_time_dialog_ok, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                // allow alphanumeric route ids (e.g. "600A") — do not strip letters here
                val route = input.text?.toString()?.trim()
                if (route.isNullOrBlank()) {
                    input.error = getString(R.string.bus_time_dialog_hint)
                    return@setOnClickListener
                }

                dialog.dismiss()
                // reuse handler logic
                pendingBusRouteQuery = null
                // ensure permission and perform lookup
                val hasLocation = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasLocation) {
                    pendingBusRouteQuery = route
                    requestStoreGeoPermissions.launch(requiredStoreGeoPermissions())
                    Toast.makeText(this, "Требуется разрешение на местоположение", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        // select router by proximity
                        busProgress.visibility = View.VISIBLE
                        val router = selectRouterForLocation(loc.latitude, loc.longitude)
                        busAnnouncer?.announceNextBus(route, loc.latitude, loc.longitude, router = router) { departure ->
                            displayBusDeparture(departure, route)
                        }
                    } else {
                        // fallback geocode
                        Thread {
                            val client = OpenMeteoClient()
                            val fallback = getWeatherFallbackCity()
                            val point = client.geocode(fallback)
                            if (point != null) {
                                busProgress.visibility = View.VISIBLE
                                busAnnouncer?.announceNextBus(route, point.latitude, point.longitude, router = selectRouterForLocation(point.latitude, point.longitude)) { departure ->
                                    displayBusDeparture(departure, route)
                                }
                            } else {
                                val msg = "Не удалось определить координаты для $fallback."
                                runOnUiThread {
                                    if (isTtsReady) tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "bus_query")
                                    else Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }.start()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun selectRouterForLocation(lat: Double, lon: Double): String {
        // First try bounding-box checks for known operator regions (faster and more reliable than plain distance)
        // HSL (Greater Helsinki) approximate bbox
        val hslMinLat = 59.8
        val hslMaxLat = 60.6
        val hslMinLon = 23.5
        val hslMaxLon = 26.0
        if (lat in hslMinLat..hslMaxLat && lon in hslMinLon..hslMaxLon) return "hsl"

        // Föli (Turku) approximate bbox
        val turkuMinLat = 60.25
        val turkuMaxLat = 60.6
        val turkuMinLon = 21.8
        val turkuMaxLon = 22.6
        if (lat in turkuMinLat..turkuMaxLat && lon in turkuMinLon..turkuMaxLon) return "turku"

        // Fallback to simple proximity to Helsinki vs Turku
        fun sqr(x: Double) = x * x
        val helsinkiLat = 60.1699
        val helsinkiLon = 24.9384
        val turkuLat = 60.4518
        val turkuLon = 22.2666
        val dH = sqr(lat - helsinkiLat) + sqr(lon - helsinkiLon)
        val dT = sqr(lat - turkuLat) + sqr(lon - turkuLon)
        return if (dH <= dT) "hsl" else "turku"
    }

    private fun parseRouteNumberFromText(text: String): String? {
        // 1) digits first
        Regex("\\d{1,4}").find(text)?.let { return it.value }

        // 1b) alphanumeric tokens containing at least one digit (e.g. 600A, 12B)
        Regex("(?i)\\b(?=.*\\d)[0-9\\p{L}-]{1,6}\\b").find(text)?.let { return it.value }

        // try words -> number (Russian support)
        WordsToNumber.parseNumber(text)?.let { return it.toString() }

        // 2) Russian words for common hundreds (сто..девятьсот)
        val ruHundreds = mapOf(
            "сто" to 100,
            "двести" to 200,
            "триста" to 300,
            "четыреста" to 400,
            "пятьсот" to 500,
            "шестьсот" to 600,
            "семьсот" to 700,
            "восемьсот" to 800,
            "девятьсот" to 900
        )
        ruHundreds.forEach { (word, value) ->
            if (text.contains(word, ignoreCase = true)) return value.toString()
            // ordinal forms: "шестисотый" etc.
            if (text.contains(word.replace("о", "о"), ignoreCase = true) && text.contains("ый", ignoreCase = true)) return value.toString()
        }

        // 3) Finnish: try forms like "kuusi sata" or "kuusi sataa" -> 600
        val fiUnits = mapOf(
            "yksi" to 1, "kaksi" to 2, "kolme" to 3, "nelja" to 4, "neljä" to 4,
            "viisi" to 5, "kuusi" to 6, "seitsemän" to 7, "kahdeksan" to 8, "yhdeksän" to 9
        )
        // if text contains e.g. "kuusi" and "sata" -> 600
        fiUnits.forEach { (w, n) ->
            if (text.contains(w, ignoreCase = true) && text.contains("sata", ignoreCase = true)) return (n * 100).toString()
        }

        // 4) fallback: look for words for exact hundreds in Finnish like "sata" (100) or "kaksisataa" (200)
        val fiHundreds = mapOf(
            "sata" to 100,
            "kaksisataa" to 200,
            "kolmesataa" to 300,
            "neljasataa" to 400,
            "viisisataa" to 500,
            "kuusisataa" to 600,
            "seitsemansataa" to 700,
            "kahdeksansataa" to 800,
            "yhdeksansataa" to 900
        )
        fiHundreds.forEach { (w, v) -> if (text.contains(w, ignoreCase = true)) return v.toString() }

        return null
    }

    private fun displayBusDeparture(departure: BusDeparture?, route: String) {
        // hide loading indicator
        busProgress.visibility = View.GONE

        if (departure == null) {
            busRouteText.text = "Маршрут $route"
            busStopText.text = "Рейсов не найдено"
            busTimeText.text = ""
            busMinutesText.text = ""
            busRealtimeText.text = ""
            busSourceText.text = ""
            return
        }

        busRouteText.text = "Маршрут ${departure.route}"
        busStopText.text = "Остановка: ${departure.stopName}"

        if (departure.minutesUntil < 0) {
            // synthetic departure: we couldn't fetch schedule for this country/provider
            busTimeText.text = getString(R.string.bus_schedule_unsupported_note)
            busMinutesText.text = ""
            busRealtimeText.text = ""
            busSourceText.text = departure.source ?: "synthetic"
            return
        }

        val dt = Instant.ofEpochMilli(departure.departureEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
        busTimeText.text = "Отправление: $dt"
        busMinutesText.text = if (departure.minutesUntil == 0) "Сейчас" else "Через ${departure.minutesUntil} мин"
        busRealtimeText.text = if (departure.realtime) "Данные: в реальном времени" else "Данные: расписание"
        busSourceText.text = departure.source ?: ""
    }

    private fun handleWastePickupCommand(rawText: String): Boolean {
        val command = WastePickupCommandParser.extract(rawText) ?: return false

        WastePickupScheduler.schedule(
            context = this,
            command = command
        )

        Toast.makeText(
            this,
            getString(
                R.string.waste_scheduled,
                command.materialLabel,
                command.dayOfWeek.name,
                command.intervalWeeks
            ),
            Toast.LENGTH_LONG
        ).show()
        parsedPreview.text = getString(
            R.string.waste_preview_template,
            command.materialLabel,
            command.dayOfWeek.name,
            command.intervalWeeks
        )
        reminderInput.text?.clear()
        return true
    }

    private fun handleExpenseLogCommand(rawText: String): Boolean {
        val command = ExpenseLogCommandParser.extract(rawText) ?: return false

        ExpenseLogStore.append(this, command)
        ExpenseWeeklyReportScheduler.ensureScheduled(this)

        val amountText = String.format(Locale.US, "%.2f", command.amountEuro)
        Toast.makeText(
            this,
            getString(R.string.expense_logged_toast, amountText, command.note),
            Toast.LENGTH_LONG
        ).show()
        parsedPreview.text = getString(R.string.expense_preview_template, amountText, command.note)
        reminderInput.text?.clear()
        return true
    }

    private fun handleChildCareCommand(rawText: String): Boolean {
        val command = ChildCareCommandParser.extract(rawText) ?: return false

        when (command) {
            is ChildCareCommand.LogEvent -> {
                val happenedAtEpoch = command
                    .happenedAt
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                ChildCareLogStore.append(
                    context = this,
                    eventType = command.eventType,
                    happenedAtEpochMillis = happenedAtEpoch,
                    note = command.note
                )

                val timeLabel = command.happenedAt.toLocalTime().format(timeFormatter)
                val response = getString(R.string.child_care_logged_toast, command.note, timeLabel)
                speakOrShowChildCareResponse(response)
                parsedPreview.text = getString(R.string.child_care_preview_log, command.note, timeLabel)
                reminderInput.text?.clear()
                return true
            }

            is ChildCareCommand.QueryLastEvent -> {
                val latest = ChildCareLogStore.findLatestByType(this, command.eventType)
                val response = if (latest == null) {
                    getString(R.string.child_care_query_not_found, childEventTypeLabel(command.eventType))
                } else {
                    val timeLabel = ChildCareLogStore.formatTime(latest.timestampEpochMillis)
                    getString(
                        R.string.child_care_query_found,
                        childEventTypeLabel(command.eventType),
                        timeLabel
                    )
                }

                speakOrShowChildCareResponse(response)
                parsedPreview.text = response
                reminderInput.text?.clear()
                return true
            }
        }
    }

    private fun speakOrShowChildCareResponse(message: String) {
        if (isNightSilentModeEnabled() && isQuietHoursNow()) {
            vibrateQuietFeedback()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }

        if (isTtsReady) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "child_care_response")
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun childEventTypeLabel(type: ChildEventType): String {
        return when (type) {
            ChildEventType.MEAL -> getString(R.string.child_care_event_meal)
            ChildEventType.VITAMINS -> getString(R.string.child_care_event_vitamins)
            ChildEventType.OTHER -> getString(R.string.child_care_event_other)
        }
    }

    private fun requestPermissionsOnFirstLaunchIfNeeded() {
        val prefs = getSharedPreferences(FIRST_LAUNCH_PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(FIRST_LAUNCH_PERMISSIONS_REQUESTED, false)) {
            return
        }

        val missingPermissions = buildList {
            if (!hasPermission(Manifest.permission.READ_CALENDAR)) add(Manifest.permission.READ_CALENDAR)
            if (!hasPermission(Manifest.permission.WRITE_CALENDAR)) add(Manifest.permission.WRITE_CALENDAR)
            if (!hasPermission(Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
        }

        if (missingPermissions.isNotEmpty()) {
            showPrePermissionDialog(missingPermissions.toTypedArray())
        } else {
            prefs.edit().putBoolean(FIRST_LAUNCH_PERMISSIONS_REQUESTED, true).apply()
        }
    }

    private fun showPrePermissionDialog(missingPermissions: Array<String>) {
        val prefs = getSharedPreferences(FIRST_LAUNCH_PREFS, MODE_PRIVATE)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permissions_intro_title)
            .setMessage(R.string.permissions_intro_message)
            .setNegativeButton(R.string.permissions_intro_later) { _, _ ->
                prefs.edit()
                    .putBoolean(FIRST_LAUNCH_PERMISSIONS_REQUESTED, true)
                    .putBoolean(FIRST_LAUNCH_PERMISSIONS_DEFERRED, true)
                    .apply()
            }
            .setPositiveButton(R.string.permissions_intro_continue) { _, _ ->
                prefs.edit()
                    .putBoolean(FIRST_LAUNCH_PERMISSIONS_REQUESTED, true)
                    .putBoolean(FIRST_LAUNCH_PERMISSIONS_DEFERRED, false)
                    .apply()
                requestInitialPermissions.launch(missingPermissions)
            }
            .show()
    }

    private fun maybeShowDeferredPermissionReminderForVoice(): Boolean {
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) return false
        if (!shouldShowDeferredPermissionReminder()) return false

        markDeferredReminderAsShown()
        showDeferredPermissionReminderDialog(
            title = getString(R.string.permissions_reminder_voice_title),
            message = getString(R.string.permissions_reminder_voice_message),
            permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        )
        return true
    }

    private fun maybeShowDeferredPermissionReminderForCalendar(): Boolean {
        val missingCalendar = buildList {
            if (!hasPermission(Manifest.permission.READ_CALENDAR)) add(Manifest.permission.READ_CALENDAR)
            if (!hasPermission(Manifest.permission.WRITE_CALENDAR)) add(Manifest.permission.WRITE_CALENDAR)
        }
        if (missingCalendar.isEmpty()) return false
        if (!shouldShowDeferredPermissionReminder()) return false

        markDeferredReminderAsShown()
        showDeferredPermissionReminderDialog(
            title = getString(R.string.permissions_reminder_calendar_title),
            message = getString(R.string.permissions_reminder_calendar_message),
            permissions = missingCalendar.toTypedArray()
        )
        return true
    }

    private fun showDeferredPermissionReminderDialog(title: String, message: String, permissions: Array<String>) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.permissions_reminder_later, null)
            .setPositiveButton(R.string.permissions_reminder_continue) { _, _ ->
                requestInitialPermissions.launch(permissions)
            }
            .show()
    }

    private fun shouldShowDeferredPermissionReminder(): Boolean {
        val prefs = getSharedPreferences(FIRST_LAUNCH_PREFS, MODE_PRIVATE)
        val deferred = prefs.getBoolean(FIRST_LAUNCH_PERMISSIONS_DEFERRED, false)
        val shown = prefs.getBoolean(FIRST_LAUNCH_GENTLE_REMINDER_SHOWN, false)
        return deferred && !shown
    }

    private fun markDeferredReminderAsShown() {
        getSharedPreferences(FIRST_LAUNCH_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(FIRST_LAUNCH_GENTLE_REMINDER_SHOWN, true)
            .putBoolean(FIRST_LAUNCH_PERMISSIONS_DEFERRED, false)
            .apply()
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isPermissionPermanentlyDenied(permission: String): Boolean {
        if (hasPermission(permission)) return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

    private fun permissionDeniedMessageRes(type: PermissionDeniedMessageType): Int {
        return when (type) {
            PermissionDeniedMessageType.MICROPHONE_TEMPORARY -> R.string.permissions_denied_microphone_settings_hint
            PermissionDeniedMessageType.CALENDAR_TEMPORARY -> R.string.permissions_denied_calendar_settings_hint
            PermissionDeniedMessageType.BOTH_TEMPORARY -> R.string.permissions_denied_both_settings_hint
            PermissionDeniedMessageType.MICROPHONE_PERMANENT -> R.string.permissions_denied_microphone_permanent_settings_hint
            PermissionDeniedMessageType.CALENDAR_PERMANENT -> R.string.permissions_denied_calendar_permanent_settings_hint
            PermissionDeniedMessageType.BOTH_PERMANENT -> R.string.permissions_denied_both_permanent_settings_hint
        }
    }

    private fun showPermissionDeniedFeedback(
        messageRes: Int,
        isPermanentDenial: Boolean,
        retryType: PermissionRetryType?,
        retryPermissions: Array<String>?
    ) {
        if (isPermanentDenial) {
            showPermanentPermissionDeniedDialog(messageRes)
        } else {
            showPermissionRetrySnackbar(messageRes, retryType, retryPermissions)
        }
    }

    private fun showPermissionRetrySnackbar(
        messageRes: Int,
        retryType: PermissionRetryType?,
        retryPermissions: Array<String>?
    ) {
        val snackbar = Snackbar.make(findViewById(R.id.main), getString(messageRes), Snackbar.LENGTH_LONG)
        if (retryType != null) {
            snackbar.setAction(R.string.permissions_retry_action) {
                when (retryType) {
                    PermissionRetryType.CALENDAR -> {
                        requestCalendarPermissions.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    }

                    PermissionRetryType.INITIAL -> {
                        if (!retryPermissions.isNullOrEmpty()) {
                            requestInitialPermissions.launch(retryPermissions)
                        }
                    }
                }
            }
        }
        snackbar.show()
    }

    private fun showPermanentPermissionDeniedDialog(messageRes: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permissions_permanent_dialog_title)
            .setMessage(getString(messageRes))
            .setNegativeButton(R.string.permissions_permanent_dialog_cancel, null)
            .setPositiveButton(R.string.open_settings_action) { _, _ ->
                openAppSettings()
            }
            .show()
    }

    private fun showPermissionPurposeDialog(titleRes: Int, messageRes: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .setPositiveButton(R.string.permissions_info_dialog_ok, null)
            .show()
    }

    private fun requestMicrophonePermissionFromStatus() {
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, getString(R.string.permission_already_granted), Toast.LENGTH_SHORT).show()
            return
        }
        requestInitialPermissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    private fun requestCalendarPermissionsFromStatus() {
        if (hasPermission(Manifest.permission.READ_CALENDAR) && hasPermission(Manifest.permission.WRITE_CALENDAR)) {
            Toast.makeText(this, getString(R.string.permission_already_granted), Toast.LENGTH_SHORT).show()
            return
        }
        requestCalendarPermissions.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun getWeatherFallbackCity(): String {
        val prefs = getSharedPreferences(WEATHER_PREFS, MODE_PRIVATE)
        return prefs.getString(WEATHER_FALLBACK_CITY, getString(R.string.weather_default_location)).orEmpty()
            .trim()
            .ifBlank { getString(R.string.weather_default_location) }
    }

    private fun setWeatherFallbackCity(city: String) {
        getSharedPreferences(WEATHER_PREFS, MODE_PRIVATE)
            .edit()
            .putString(WEATHER_FALLBACK_CITY, city)
            .apply()
    }

    private fun updateWeatherFallbackCityUi() {
        weatherFallbackCityText.text = getString(R.string.weather_fallback_city_value, getWeatherFallbackCity())
    }

    private fun showWeatherFallbackCityDialog() {
        val input = EditText(this).apply {
            setText(getWeatherFallbackCity())
            setSelection(text?.length ?: 0)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.weather_fallback_city_title)
            .setView(input)
            .setNegativeButton(R.string.confirm_dialog_cancel, null)
            .setPositiveButton(R.string.confirm_dialog_save, null)
            .create()

        input.doAfterTextChanged {
            input.error = null
        }

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val city = input.text?.toString().orEmpty().trim()
                if (city.length < 2) {
                    input.error = getString(R.string.weather_fallback_city_error)
                    return@setOnClickListener
                }

                setWeatherFallbackCity(CityNameNormalizer.normalize(city))
                updateWeatherFallbackCityUi()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun createReminderParser(): ReminderParser {
        return ReminderParser(
            defaultTimeProvider = { getConfiguredDefaultReminderTime() },
            defaultEventTitleProvider = { getString(R.string.default_event_title) }
        )
    }

    private fun getSettingsPrefs() = getSharedPreferences(APP_SETTINGS_PREFS, MODE_PRIVATE)

    private fun updateTireSettingsUi() {
        tireFreezeThresholdText.text = getString(
            R.string.tire_settings_threshold_value,
            getTireFreezeThresholdC().toInt().toString()
        )

        val summerDate = formatDayMonth(getTireSummerDay(), getTireSummerMonth())
        val winterDate = formatDayMonth(getTireWinterDay(), getTireWinterMonth())
        tirePolicyText.text = getString(R.string.tire_settings_policy_value, summerDate, winterDate)
    }

    private fun showTireFreezeThresholdDialog() {
        val values = doubleArrayOf(0.0, -2.0)
        val labels = arrayOf(
            getString(R.string.tire_settings_threshold_option_zero),
            getString(R.string.tire_settings_threshold_option_minus_two)
        )
        var selected = if (getTireFreezeThresholdC() <= -2.0) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tire_settings_threshold_dialog_title)
            .setSingleChoiceItems(labels, selected) { _, which ->
                selected = which
            }
            .setNegativeButton(R.string.confirm_dialog_cancel, null)
            .setPositiveButton(R.string.confirm_dialog_save) { _, _ ->
                setTireFreezeThresholdC(values[selected])
                updateTireSettingsUi()
            }
            .show()
    }

    private fun showTirePolicyDatesDialog() {
        var summerDate = runCatching {
            LocalDate.of(LocalDate.now().year, getTireSummerMonth(), getTireSummerDay())
        }.getOrElse {
            LocalDate.of(LocalDate.now().year, TireChangePolicy.DEFAULT_SUMMER_MONTH, TireChangePolicy.DEFAULT_SUMMER_DAY)
        }
        var winterDate = runCatching {
            LocalDate.of(LocalDate.now().year, getTireWinterMonth(), getTireWinterDay())
        }.getOrElse {
            LocalDate.of(LocalDate.now().year, TireChangePolicy.DEFAULT_WINTER_MONTH, TireChangePolicy.DEFAULT_WINTER_DAY)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val summerInput = createPolicyDateInput(
            hintRes = R.string.tire_settings_policy_summer_hint,
            initialDate = summerDate,
            pickerTag = "tire_policy_summer_picker"
        ) { picked ->
            summerDate = picked
        }

        val winterInput = createPolicyDateInput(
            hintRes = R.string.tire_settings_policy_winter_hint,
            initialDate = winterDate,
            pickerTag = "tire_policy_winter_picker"
        ) { picked ->
            winterDate = picked
        }

        container.addView(summerInput)
        container.addView(winterInput)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tire_settings_policy_dialog_title)
            .setView(container)
            .setNegativeButton(R.string.confirm_dialog_cancel, null)
            .setPositiveButton(R.string.confirm_dialog_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                setTirePolicyDates(
                    summerDay = summerDate.dayOfMonth,
                    summerMonth = summerDate.monthValue,
                    winterDay = winterDate.dayOfMonth,
                    winterMonth = winterDate.monthValue
                )
                updateTireSettingsUi()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun getTireFreezeThresholdC(): Double {
        return getSettingsPrefs().getFloat(TIRE_FREEZE_THRESHOLD_C, 0f).toDouble()
    }

    private fun setTireFreezeThresholdC(value: Double) {
        getSettingsPrefs().edit().putFloat(TIRE_FREEZE_THRESHOLD_C, value.toFloat()).apply()
    }

    private fun getTireSummerDay(): Int {
        return getSettingsPrefs().getInt(TIRE_SUMMER_DAY, TireChangePolicy.DEFAULT_SUMMER_DAY)
    }

    private fun getTireSummerMonth(): Int {
        return getSettingsPrefs().getInt(TIRE_SUMMER_MONTH, TireChangePolicy.DEFAULT_SUMMER_MONTH)
    }

    private fun getTireWinterDay(): Int {
        return getSettingsPrefs().getInt(TIRE_WINTER_DAY, TireChangePolicy.DEFAULT_WINTER_DAY)
    }

    private fun getTireWinterMonth(): Int {
        return getSettingsPrefs().getInt(TIRE_WINTER_MONTH, TireChangePolicy.DEFAULT_WINTER_MONTH)
    }

    private fun createPolicyDateInput(
        hintRes: Int,
        initialDate: LocalDate,
        pickerTag: String,
        onDateChanged: (LocalDate) -> Unit
    ): EditText {
        var selectedDate = initialDate
        return EditText(this).apply {
            hint = getString(hintRes)
            isFocusable = false
            isClickable = true
            setText(formatDayMonth(selectedDate.dayOfMonth, selectedDate.monthValue))
            setOnClickListener {
                openDatePickerUnrestricted(selectedDate, pickerTag) { picked ->
                    selectedDate = picked
                    setText(formatDayMonth(selectedDate.dayOfMonth, selectedDate.monthValue))
                    onDateChanged(picked)
                }
            }
        }
    }

    private fun setTirePolicyDates(
        summerDay: Int,
        summerMonth: Int,
        winterDay: Int,
        winterMonth: Int
    ) {
        getSettingsPrefs().edit()
            .putInt(TIRE_SUMMER_DAY, summerDay)
            .putInt(TIRE_SUMMER_MONTH, summerMonth)
            .putInt(TIRE_WINTER_DAY, winterDay)
            .putInt(TIRE_WINTER_MONTH, winterMonth)
            .apply()
    }


    private fun formatDayMonth(day: Int, month: Int): String {
        return String.format(Locale.getDefault(), "%02d.%02d", day, month)
    }

    private fun isHandsFreeVoiceConfirmationEnabled(): Boolean {
        return getSettingsPrefs().getBoolean(HANDS_FREE_VOICE_CONFIRMATION_ENABLED, true)
    }

    private fun setHandsFreeVoiceConfirmationEnabled(enabled: Boolean) {
        getSettingsPrefs()
            .edit()
            .putBoolean(HANDS_FREE_VOICE_CONFIRMATION_ENABLED, enabled)
            .apply()
    }

    private fun updateHandsFreeVoiceStatusUi(isEnabled: Boolean) {
        applyToggleStatusUi(
            statusTextView = handsFreeVoiceStatusText,
            isEnabled = isEnabled,
            onTextRes = R.string.hands_free_voice_status_on,
            offTextRes = R.string.hands_free_voice_status_off,
            animate = true
        )
    }

    private fun applyToggleStatusUi(
        statusTextView: TextView,
        isEnabled: Boolean,
        onTextRes: Int,
        offTextRes: Int,
        animate: Boolean
    ) {
        statusTextView.text = if (isEnabled) getString(onTextRes) else getString(offTextRes)

        val statusIcon = if (isEnabled) R.drawable.ic_status_on_18 else R.drawable.ic_status_off_18
        statusTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(statusIcon, 0, 0, 0)

        val statusColor = if (isEnabled) {
            ContextCompat.getColor(this, R.color.permission_status_granted)
        } else {
            ContextCompat.getColor(this, R.color.status_neutral)
        }
        statusTextView.setTextColor(statusColor)

        if (animate) {
            statusTextView.alpha = 0.7f
            statusTextView.animate()
                .alpha(1f)
                .setDuration(180)
                .start()
        }
    }


    private fun getConfiguredDefaultReminderTime(): LocalTime {
        val raw = getSettingsPrefs().getString(DEFAULT_REMINDER_TIME, LocalTime.of(9, 0).toString())
        return runCatching { LocalTime.parse(raw) }.getOrDefault(LocalTime.of(9, 0))
    }

    private fun setConfiguredDefaultReminderTime(time: LocalTime) {
        getSettingsPrefs()
            .edit()
            .putString(DEFAULT_REMINDER_TIME, time.withSecond(0).withNano(0).toString())
            .apply()
    }

    private fun updateDefaultReminderTimeUi() {
        val value = getConfiguredDefaultReminderTime().format(timeFormatter)
        defaultReminderTimeText.text = getString(R.string.default_reminder_time_value, value)
    }

    private fun showDefaultReminderTimeDialog() {
        openTimePicker(getConfiguredDefaultReminderTime()) { pickedTime ->
            setConfiguredDefaultReminderTime(pickedTime)
            parser = createReminderParser()
            updateDefaultReminderTimeUi()

            val currentText = reminderInput.text?.toString().orEmpty()
            if (currentText.isNotBlank()) {
                updatePreview(currentText)
            }
        }
    }

    private fun getSahkoNightStartHour(): Int {
        return getSettingsPrefs().getInt(SAHKO_NIGHT_START_HOUR, 22).coerceIn(0, 23)
    }

    private fun getSahkoNightEndHour(): Int {
        return getSettingsPrefs().getInt(SAHKO_NIGHT_END_HOUR, 7).coerceIn(0, 23)
    }

    private fun setSahkoNightWindow(startHour: Int, endHour: Int) {
        getSettingsPrefs().edit()
            .putInt(SAHKO_NIGHT_START_HOUR, startHour.coerceIn(0, 23))
            .putInt(SAHKO_NIGHT_END_HOUR, endHour.coerceIn(0, 23))
            .apply()
    }

    private fun updateSahkoNightWindowUi() {
        val start = LocalTime.of(getSahkoNightStartHour(), 0).format(timeFormatter)
        val end = LocalTime.of(getSahkoNightEndHour(), 0).format(timeFormatter)
        sahkoNightWindowText.text = getString(R.string.sahko_night_window_value, start, end)
    }

    private fun showSahkoNightWindowDialog() {
        var selectedStart = LocalTime.of(getSahkoNightStartHour(), 0)
        var selectedEnd = LocalTime.of(getSahkoNightEndHour(), 0)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val startInput = EditText(this).apply {
            hint = getString(R.string.sahko_night_start_hint)
            isFocusable = false
            isClickable = true
            setText(selectedStart.format(timeFormatter))
            setOnClickListener {
                openTimePicker(selectedStart) { picked ->
                    selectedStart = picked.withMinute(0)
                    setText(selectedStart.format(timeFormatter))
                }
            }
        }

        val endInput = EditText(this).apply {
            hint = getString(R.string.sahko_night_end_hint)
            isFocusable = false
            isClickable = true
            setText(selectedEnd.format(timeFormatter))
            setOnClickListener {
                openTimePicker(selectedEnd) { picked ->
                    selectedEnd = picked.withMinute(0)
                    setText(selectedEnd.format(timeFormatter))
                }
            }
        }

        container.addView(startInput)
        container.addView(endInput)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sahko_night_window_title)
            .setView(container)
            .setNegativeButton(R.string.confirm_dialog_cancel, null)
            .setPositiveButton(R.string.confirm_dialog_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                setSahkoNightWindow(selectedStart.hour, selectedEnd.hour)
                updateSahkoNightWindowUi()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showDirectionsAction(reminder: ParsedReminder) {
        val location = reminder.location?.trim()?.replace(" ", "+").orEmpty()
        if (location.isBlank()) {
            return
        }

        Snackbar.make(
            findViewById(R.id.main),
            getString(R.string.route_snackbar_prompt, location),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.route_action) {
                openDirections(location, reminder.eventDateTime)
            }
            .show()
    }

    private fun openDirections(destination: String, arrivalDateTime: LocalDateTime) {
        val normalizedDestination = destination.trim()
        if (normalizedDestination.isBlank()) {
            Toast.makeText(this, getString(R.string.route_location_missing), Toast.LENGTH_LONG).show()
            return
        }

        val routeLinks = RouteLinkBuilder.build(
            destination = normalizedDestination,
            arrivalDateTime = arrivalDateTime,
            zoneId = ZoneId.systemDefault()
        )

        val hslAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(routeLinks.hslAppUrl)).apply {
            setPackage(HSL_PACKAGE)
        }
        val hslWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse(routeLinks.hslWebUrl))

        if (startIfResolvable(hslAppIntent) || startIfResolvable(hslWebIntent)) {
            return
        }

        val mapsAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(routeLinks.googleMapsUrl)).apply {
            setPackage(GOOGLE_MAPS_PACKAGE)
        }
        val mapsAnyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(routeLinks.googleMapsUrl))

        if (!startIfResolvable(mapsAppIntent) && !startIfResolvable(mapsAnyIntent)) {
            Toast.makeText(this, getString(R.string.route_no_app_found), Toast.LENGTH_LONG).show()
        }
    }

    private fun startIfResolvable(intent: Intent): Boolean {
        val resolved = intent.resolveActivity(packageManager) != null
        if (!resolved) return false

        startActivity(intent)
        return true
    }


    private fun updatePermissionsStatusUi() {
        val microphoneGranted = hasPermission(Manifest.permission.RECORD_AUDIO)
        val calendarGranted = hasPermission(Manifest.permission.READ_CALENDAR) &&
            hasPermission(Manifest.permission.WRITE_CALENDAR)

        val microphoneStatus = if (microphoneGranted) {
            getString(R.string.permissions_status_granted_marked)
        } else {
            getString(R.string.permissions_status_denied_marked)
        }

        val calendarStatus = if (calendarGranted) {
            getString(R.string.permissions_status_granted_marked)
        } else {
            getString(R.string.permissions_status_denied_marked)
        }

        val grantedColor = ContextCompat.getColor(this, R.color.permission_status_granted)
        val deniedColor = ContextCompat.getColor(this, R.color.permission_status_denied)

        permissionsMicStatusText.text = getString(R.string.permissions_status_microphone_template, microphoneStatus)
        permissionsCalendarStatusText.text = getString(R.string.permissions_status_calendar_template, calendarStatus)
        permissionsMicStatusText.setTextColor(if (microphoneGranted) grantedColor else deniedColor)
        permissionsCalendarStatusText.setTextColor(if (calendarGranted) grantedColor else deniedColor)
    }

    private fun showDraftRestoredSnackbar() {
        val reminder = pendingReminder
        val snackbar = Snackbar.make(findViewById(R.id.main), getString(R.string.draft_restored_message), Snackbar.LENGTH_LONG)

        if (reminder != null) {
            snackbar.setAction(R.string.draft_restored_edit_action) {
                showConfirmationDialog(reminder)
            }
        } else {
            snackbar.setAction(R.string.draft_restored_clear_action) {
                clearDraftUiState()
            }
        }

        snackbar.show()
    }

    private fun clearDraftUiState() {
        lastClearedDraftSnapshot = DraftSnapshot(
            inputText = reminderInput.text?.toString().orEmpty(),
            pendingReminder = pendingReminder
        )

        reminderInput.text?.clear()
        pendingReminder = null
        parsedPreview.text = getString(R.string.preview_empty)
        previewRouteButton.isEnabled = false
        updatePendingEditVisibility()
        clearDraftFromPrefs()

        if (lastClearedDraftSnapshot?.hasContent() == true) {
            showDraftClearedUndoSnackbar()
        }
    }

    private fun showClearDraftConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_draft_confirm_title)
            .setMessage(R.string.clear_draft_confirm_message)
            .setNegativeButton(R.string.clear_draft_confirm_cancel, null)
            .setPositiveButton(R.string.clear_draft_confirm_action) { _, _ ->
                clearDraftUiState()
            }
            .show()
    }

    private fun showDraftClearedUndoSnackbar() {
        Snackbar.make(findViewById(R.id.main), getString(R.string.draft_cleared_message), Snackbar.LENGTH_LONG)
            .setAction(R.string.draft_cleared_undo_action) {
                restoreLastClearedDraft()
            }
            .show()
    }

    private fun restoreLastClearedDraft() {
        val snapshot = lastClearedDraftSnapshot ?: return

        reminderInput.setText(snapshot.inputText)
        pendingReminder = snapshot.pendingReminder

        if (snapshot.inputText.isNotBlank()) {
            updatePreview(snapshot.inputText)
        } else {
            parsedPreview.text = getString(R.string.preview_empty)
        }

        updatePendingEditVisibility()
        saveDraftToPrefs()
        lastClearedDraftSnapshot = null
    }
}

private data class DraftSnapshot(
    val inputText: String,
    val pendingReminder: ParsedReminder?
) {
    fun hasContent(): Boolean = inputText.isNotBlank() || pendingReminder != null
}

private data class StoreGeoRequest(
    val eventId: Long,
    val placeName: String
)

private data class ParcelGeoRequest(
    val itemLabel: String,
    val pickupPlace: String,
    val deadlineAt: LocalDateTime
)

