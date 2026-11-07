package com.loantrack.app.ui.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.loantrack.app.R
import com.loantrack.app.util.BiometricHelper
import com.loantrack.app.util.SecurityPrefs as AppSecurityPrefs

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showLegalDialog by remember { mutableStateOf<LegalDocType?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data)
        }
    }

    // Trigger biometric automaticamente quando há sessão Firebase + biometria ativada
    LaunchedEffect(uiState.triggerBiometric) {
        if (uiState.triggerBiometric && !uiState.isLocked) {
            val activity = context as? FragmentActivity ?: return@LaunchedEffect
            BiometricHelper.authenticate(
                activity = activity,
                title = "LoanTrack",
                subtitle = "Confirme sua identidade para acessar",
                onSuccess = { viewModel.onBiometricSuccess() },
                onError = { viewModel.onBiometricFailed() },
                onFailed = { viewModel.onBiometricFailed() }
            )
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onLoginSuccess()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Dialog: ativar biometria após primeiro login
    if (uiState.showEnableBiometricPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.skipBiometric() },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text("Ativar login biométrico?") },
            text = { Text("Nas próximas aberturas, você poderá entrar usando biometria ou PIN do dispositivo, sem precisar fazer login com o Google novamente.") },
            confirmButton = {
                TextButton(onClick = { viewModel.enableBiometric() }) {
                    Text("Ativar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipBiometric() }) {
                    Text("Agora não")
                }
            }
        )
    }

    // Dialogs legais
    showLegalDialog?.let { docType ->
        LegalDialog(type = docType, onDismiss = { showLegalDialog = null })
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Ícone e nome
            Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LoanTrack",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gerencie seus empréstimos pessoais",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.isLocked) {
                // Tela de bloqueio
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Conta temporariamente bloqueada",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aguarde ${uiState.lockRemainingSeconds / 60}:${"%02d".format(uiState.lockRemainingSeconds % 60)} para tentar novamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Limite de ${AppSecurityPrefs.MAX_ATTEMPTS} tentativas atingido.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Botão biometria (se sessão existe e biometria está ativada)
                if (uiState.hasFirebaseSession && uiState.biometricEnabled) {
                    Button(
                        onClick = { viewModel.retriggerBiometric() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Entrar com biometria", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            val gso = buildGso(context)
                            val client = GoogleSignIn.getClient(context, gso)
                            client.signOut()
                            viewModel.signOut()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Usar outra conta Google", fontSize = 15.sp)
                    }
                } else {
                    // Aceite de termos (somente se nunca aceitou)
                    if (!uiState.hasAcceptedTerms) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = uiState.termsChecked,
                                onCheckedChange = { viewModel.updateTermsChecked(it) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    append("Li e concordo com os ")
                                    withStyle(SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )) {
                                        append("Termos de Uso")
                                    }
                                    append(" e a ")
                                    withStyle(SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )) {
                                        append("Política de Privacidade")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.clickable { showLegalDialog = LegalDocType.TERMS }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Botão Google
                    Button(
                        onClick = {
                            if (!uiState.hasAcceptedTerms && !uiState.termsChecked) return@Button
                            if (!uiState.hasAcceptedTerms) viewModel.acceptTermsAndProceed()
                            val gso = buildGso(context)
                            launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                        },
                        enabled = uiState.hasAcceptedTerms || uiState.termsChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Entrar com Google", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Rodapé com links legais
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Termos de Uso",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { showLegalDialog = LegalDocType.TERMS }
                )
                Text(
                    text = "  •  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Text(
                    text = "Política de Privacidade",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { showLegalDialog = LegalDocType.PRIVACY }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun buildGso(context: android.content.Context) =
    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
