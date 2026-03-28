package com.example.planora.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

// ─── Design tokens ─────────────────────────────────────────────────────────────
private val SBG       = Color(0xFF060608)
private val SSurface1 = Color(0xFF111116)
private val SSurface2 = Color(0xFF1A1A22)
private val SSurface3 = Color(0xFF22222E)
private val SBorder   = Color(0xFF2A2A38)

private val SBlue   = Color(0xFF6C8FFF)
private val SGreen  = Color(0xFF34D48C)
private val SRose   = Color(0xFFFF6B8A)
private val SAmber  = Color(0xFFFFB547)
private val SPurple = Color(0xFFAA80FF)

private val STextPrimary   = Color(0xFFEEEEF6)
private val STextSecondary = Color(0xFF8888A8)
private val STextTertiary  = Color(0xFF444458)

// ─── Settings Screen ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val auth    = FirebaseAuth.getInstance()
    val db      = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val user    = auth.currentUser
    val userId  = user?.uid ?: ""

    // FIX 1: displayName as mutable state so it refreshes after a successful save
    var displayName by remember { mutableStateOf(user?.displayName ?: "User") }
    val email       = user?.email ?: ""
    val firstLetter = (displayName.firstOrNull() ?: 'U').uppercaseChar().toString()

    // Dialog state
    var showEditNameDialog   by remember { mutableStateOf(false) }
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showDeleteDialog     by remember { mutableStateOf(false) }
    var showSignOutDialog    by remember { mutableStateOf(false) }

    // FIX 3: Preferences loaded from and saved to Firestore so they persist
    var notificationsEnabled by remember { mutableStateOf(true) }
    var dailyReminderEnabled by remember { mutableStateOf(false) }
    var showCompletedTasks   by remember { mutableStateOf(true) }
    var prefsLoaded          by remember { mutableStateOf(false) }

    val prefsRef = remember(userId) {
        if (userId.isNotEmpty())
            db.collection("users").document(userId).collection("settings").document("preferences")
        else null
    }

    // Load preferences from Firestore once
    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect
        prefsRef?.get()
            ?.addOnSuccessListener { doc ->
                if (doc.exists()) {
                    notificationsEnabled = doc.getBoolean("notificationsEnabled") ?: true
                    dailyReminderEnabled = doc.getBoolean("dailyReminderEnabled") ?: false
                    showCompletedTasks   = doc.getBoolean("showCompletedTasks")   ?: true
                }
                prefsLoaded = true
            }
            ?.addOnFailureListener { prefsLoaded = true }
    }

    // Save a single preference key to Firestore
    fun savePref(key: String, value: Boolean) {
        prefsRef?.set(
            mapOf(
                "notificationsEnabled" to notificationsEnabled,
                "dailyReminderEnabled" to dailyReminderEnabled,
                "showCompletedTasks"   to showCompletedTasks
            ).toMutableMap().also { it[key] = value },
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SBG)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Settings",
                fontWeight = FontWeight.Bold,
                fontSize   = 22.sp,
                color      = STextPrimary,
                modifier   = Modifier.align(Alignment.CenterStart)
            )
        }

        // ── Profile Card ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(SBlue.copy(0.16f), SPurple.copy(0.10f))))
                .border(1.dp, SBlue.copy(0.22f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar — letter updates immediately when displayName changes (FIX 1)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SBlue.copy(0.5f), SPurple.copy(0.5f))))
                        .border(2.dp, SBlue.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(firstLetter, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = STextPrimary)
                }

                Column(modifier = Modifier.weight(1f)) {
                    // This text now recomposes when displayName state changes (FIX 1)
                    Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = STextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(email, fontSize = 13.sp, color = STextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SBlue.copy(0.15f))
                            .border(1.dp, SBlue.copy(0.3f), RoundedCornerShape(8.dp))
                            .clickable { showEditNameDialog = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, null, tint = SBlue, modifier = Modifier.size(12.dp))
                            Text("Edit Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SBlue)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Preferences ───────────────────────────────────────────────────────
        SectionHeader("Preferences")

        SettingsGroup {
            ToggleRow(
                icon     = Icons.Outlined.Notifications,
                iconBg   = SBlue,
                title    = "Notifications",
                subtitle = "Enable push notifications",
                checked  = notificationsEnabled,
                onToggle = {
                    notificationsEnabled = it
                    savePref("notificationsEnabled", it)   // FIX 3
                }
            )
            GroupDivider()
            ToggleRow(
                icon     = Icons.Outlined.Alarm,
                iconBg   = SPurple,
                title    = "Daily Reminder",
                subtitle = "Get a nudge to check your tasks",
                checked  = dailyReminderEnabled,
                onToggle = {
                    dailyReminderEnabled = it
                    savePref("dailyReminderEnabled", it)   // FIX 3
                }
            )
            GroupDivider()
            ToggleRow(
                icon     = Icons.Outlined.CheckCircle,
                iconBg   = SGreen,
                title    = "Show Completed Tasks",
                subtitle = "Display finished tasks at the bottom",
                checked  = showCompletedTasks,
                onToggle = {
                    showCompletedTasks = it
                    savePref("showCompletedTasks", it)     // FIX 3
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Account ───────────────────────────────────────────────────────────
        SectionHeader("Account")

        SettingsGroup {
            ActionRow(
                icon     = Icons.Outlined.Lock,
                iconBg   = SAmber,
                title    = "Change Password",
                subtitle = "Send a reset link to your email",
                onClick  = { showChangePassDialog = true }
            )
            GroupDivider()
            ActionRow(
                icon     = Icons.Outlined.ExitToApp,
                iconBg   = STextTertiary,
                title    = "Sign Out",
                subtitle = "Log out of your account",
                onClick  = { showSignOutDialog = true }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── About ─────────────────────────────────────────────────────────────
        SectionHeader("About")

        SettingsGroup {
            ActionRow(
                icon        = Icons.Outlined.Info,
                iconBg      = SBlue,
                title       = "Version",
                subtitle    = "Planora 1.0.0",
                showChevron = false,
                onClick     = {}
            )
            GroupDivider()
            // FIX 5: replaced Icons.Outlined.Policy → Icons.Outlined.PrivacyTip
            ActionRow(
                icon     = Icons.Outlined.PrivacyTip,
                iconBg   = SPurple,
                title    = "Privacy Policy",
                subtitle = "Read how we handle your data",
                onClick  = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy"))
                    )
                }
            )
            GroupDivider()
            // FIX 5: replaced Icons.Outlined.Description → Icons.Outlined.Article
            ActionRow(
                icon     = Icons.Outlined.Article,
                iconBg   = SGreen,
                title    = "Terms of Service",
                subtitle = "Review terms and conditions",
                onClick  = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/terms"))
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Danger Zone ───────────────────────────────────────────────────────
        SectionHeader("Danger Zone")

        SettingsGroup {
            ActionRow(
                icon       = Icons.Outlined.DeleteForever,
                iconBg     = SRose,
                title      = "Delete Account",
                subtitle   = "Permanently remove your data",
                titleColor = SRose,
                onClick    = { showDeleteDialog = true }
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Made with ♥ · Planora",
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize  = 11.sp,
            color     = STextTertiary
        )
        Spacer(Modifier.height(16.dp))
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = displayName,
            onDismiss   = { showEditNameDialog = false },
            onConfirm   = { newName ->
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName.trim())
                    .build()
                user?.updateProfile(req)
                    ?.addOnSuccessListener {
                        // FIX 1: update local state so the card recomposes immediately
                        displayName = newName.trim()
                        Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                        showEditNameDialog = false
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    if (showChangePassDialog) {
        ConfirmActionDialog(
            icon         = Icons.Outlined.Lock,
            iconBg       = SAmber,
            title        = "Reset Password",
            // FIX 4: guard against empty email
            message      = if (email.isNotEmpty())
                "A password reset link will be sent to:\n$email"
            else
                "No email found on this account.",
            confirmLabel = "Send Link",
            confirmColor = SAmber,
            onDismiss    = { showChangePassDialog = false },
            onConfirm    = {
                if (email.isEmpty()) {
                    Toast.makeText(context, "No email on account", Toast.LENGTH_SHORT).show()
                    showChangePassDialog = false
                    return@ConfirmActionDialog
                }
                // FIX 4: close dialog only inside the async callbacks, not before them
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Reset email sent", Toast.LENGTH_SHORT).show()
                        showChangePassDialog = false
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        showChangePassDialog = false
                    }
            }
        )
    }

    if (showSignOutDialog) {
        ConfirmActionDialog(
            icon         = Icons.Outlined.ExitToApp,
            iconBg       = STextTertiary,
            title        = "Sign Out",
            message      = "You'll be taken back to the login screen.",
            confirmLabel = "Sign Out",
            confirmColor = STextSecondary,
            onDismiss    = { showSignOutDialog = false },
            onConfirm    = {
                auth.signOut()
                showSignOutDialog = false
                navController.navigate("login") { popUpTo(0) }
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmActionDialog(
            icon         = Icons.Outlined.DeleteForever,
            iconBg       = SRose,
            title        = "Delete Account",
            message      = "This will permanently delete your account and all data including tasks, habits and logs. This cannot be undone.",
            confirmLabel = "Delete Forever",
            confirmColor = SRose,
            onDismiss    = { showDeleteDialog = false },
            onConfirm    = {
                if (userId.isEmpty()) return@ConfirmActionDialog
                val userDocRef = db.collection("users").document(userId)

                // FIX 2: delete subcollections before deleting the user doc and auth account
                // Firestore does NOT cascade-delete subcollections automatically.
                val tasksRef   = userDocRef.collection("tasks")
                val habitsRef  = userDocRef.collection("habits")
                val settingsRef = userDocRef.collection("settings")

                // Delete tasks
                tasksRef.get().addOnSuccessListener { snap ->
                    val batch = db.batch()
                    snap.documents.forEach { batch.delete(it.reference) }
                    batch.commit()
                }

                // Delete habit logs then the habits themselves
                habitsRef.get().addOnSuccessListener { snap ->
                    snap.documents.forEach { habitDoc ->
                        habitDoc.reference.collection("logs").get()
                            .addOnSuccessListener { logSnap ->
                                val batch = db.batch()
                                logSnap.documents.forEach { batch.delete(it.reference) }
                                batch.commit()
                            }
                        habitDoc.reference.delete()
                    }
                }

                // Delete settings subcollection
                settingsRef.get().addOnSuccessListener { snap ->
                    val batch = db.batch()
                    snap.documents.forEach { batch.delete(it.reference) }
                    batch.commit()
                }

                // Finally delete the top-level user doc, then the Auth account
                userDocRef.delete().addOnCompleteListener {
                    user?.delete()
                        ?.addOnSuccessListener {
                            showDeleteDialog = false
                            navController.navigate("login") { popUpTo(0) }
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(
                                context,
                                "Re-sign-in required before deleting. Please sign out and back in.",
                                Toast.LENGTH_LONG
                            ).show()
                            showDeleteDialog = false
                        }
                }
            }
        )
    }
}

// ─── Layout helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color         = STextTertiary,
        modifier      = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SSurface1)
            .border(1.dp, SBorder, RoundedCornerShape(18.dp)),
        content  = content
    )
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(color = SBorder, modifier = Modifier.padding(start = 56.dp))
}

@Composable
private fun IconBubble(icon: ImageVector, bg: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg.copy(0.15f))
            .border(1.dp, bg.copy(0.25f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = bg, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ToggleRow(
    icon:     ImageVector,
    iconBg:   Color,
    title:    String,
    subtitle: String,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(icon = icon, bg = iconBg)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = STextPrimary)
            Text(subtitle, fontSize = 11.sp, color = STextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = SBlue,
                checkedBorderColor   = SBlue,
                uncheckedThumbColor  = STextTertiary,
                uncheckedTrackColor  = SSurface3,
                uncheckedBorderColor = SBorder
            )
        )
    }
}

@Composable
private fun ActionRow(
    icon:        ImageVector,
    iconBg:      Color,
    title:       String,
    subtitle:    String,
    titleColor:  Color   = STextPrimary,
    showChevron: Boolean = true,
    onClick:     () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(icon = icon, bg = iconBg)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            Text(subtitle, fontSize = 11.sp, color = STextSecondary)
        }
        if (showChevron) {
            Icon(Icons.Filled.ChevronRight, null, tint = STextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Edit Name Dialog ──────────────────────────────────────────────────────────
@Composable
private fun EditNameDialog(
    currentName: String,
    onDismiss:   () -> Unit,
    onConfirm:   (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SSurface1)
                .border(1.dp, SBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconBubble(Icons.Outlined.Edit, SBlue)
                    Text("Edit Name", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = STextPrimary)
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Display Name", color = STextSecondary, fontSize = 12.sp) },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = SBlue.copy(0.6f),
                        unfocusedBorderColor    = SBorder,
                        focusedTextColor        = STextPrimary,
                        unfocusedTextColor      = STextPrimary,
                        cursorColor             = SBlue,
                        focusedContainerColor   = SSurface2,
                        unfocusedContainerColor = SSurface2,
                        focusedLabelColor       = SBlue,
                        unfocusedLabelColor     = STextSecondary
                    )
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, SBorder),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = STextSecondary)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick  = { if (name.isNotBlank()) onConfirm(name) },
                        // Disabled until the name is actually different from current
                        enabled  = name.isNotBlank() && name.trim() != currentName.trim(),
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = SBlue,
                            contentColor           = Color.White,
                            disabledContainerColor = SSurface3,
                            disabledContentColor   = STextTertiary
                        )
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─── Generic Confirm Dialog ────────────────────────────────────────────────────
@Composable
private fun ConfirmActionDialog(
    icon:         ImageVector,
    iconBg:       Color,
    title:        String,
    message:      String,
    confirmLabel: String,
    confirmColor: Color,
    onDismiss:    () -> Unit,
    onConfirm:    () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SSurface1)
                .border(1.dp, SBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                IconBubble(icon = icon, bg = iconBg)
                Spacer(Modifier.height(14.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = STextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(message, fontSize = 13.sp, color = STextSecondary, lineHeight = 18.sp)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, SBorder),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = STextSecondary)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = confirmColor,
                            contentColor   = Color.White
                        )
                    ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}