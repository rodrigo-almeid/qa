package com.loantrack.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class LegalDocType { TERMS, PRIVACY }

@Composable
fun LegalDialog(
    type: LegalDocType,
    onDismiss: () -> Unit
) {
    val title = if (type == LegalDocType.TERMS) "Termos de Uso" else "Política de Privacidade"
    val content = if (type == LegalDocType.TERMS) termsText() else privacyText()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

private fun termsText() = """
LoanTrack — Termos de Uso
Última atualização: novembro de 2026

1. ACEITAÇÃO DOS TERMOS
Ao utilizar o aplicativo LoanTrack, você concorda com os presentes Termos de Uso. Caso não concorde, não utilize o aplicativo.

2. DESCRIÇÃO DO SERVIÇO
O LoanTrack é um aplicativo pessoal para controle e acompanhamento de empréstimos informais realizados pelo próprio usuário. O aplicativo não realiza intermediação financeira nem constitui atividade bancária ou creditícia regulamentada.

3. USO ADEQUADO
O usuário se compromete a:
• Utilizar o aplicativo apenas para fins lícitos e pessoais.
• Não utilizar os dados de terceiros sem consentimento.
• Manter seus dados de acesso em sigilo.
• Não tentar violar a segurança do sistema.

4. RESPONSABILIDADES
O LoanTrack não se responsabiliza por:
• Decisões financeiras tomadas com base nos dados inseridos.
• Eventuais disputas entre o usuário e os devedores.
• Perda de dados por falha de dispositivo ou conta Google.

5. PROPRIEDADE INTELECTUAL
Todo o código-fonte, design e funcionalidades do LoanTrack são de propriedade do desenvolvedor. É vedada a reprodução sem autorização expressa.

6. ALTERAÇÕES NOS TERMOS
Estes Termos podem ser atualizados a qualquer momento. O uso continuado após as alterações implica aceitação das novas condições.

7. CONTATO
Para dúvidas: almeidainteligencia@gmail.com
""".trimIndent()

private fun privacyText() = """
LoanTrack — Política de Privacidade (LGPD)
Última atualização: novembro de 2026

1. RESPONSÁVEL PELO TRATAMENTO
Os dados coletados pelo LoanTrack são tratados pelo próprio usuário do aplicativo, sob sua responsabilidade.

2. DADOS COLETADOS
O aplicativo coleta e armazena:
• Dados de autenticação Google (nome, e-mail, foto de perfil) — fornecidos pelo Google OAuth.
• Dados inseridos pelo usuário: nome do devedor, contato (telefone), valores, datas e observações.

3. FINALIDADE DO TRATAMENTO
Os dados são utilizados exclusivamente para:
• Autenticação e identificação do usuário.
• Exibição, organização e controle dos empréstimos cadastrados.
• Envio de relatórios por e-mail, quando solicitado pelo usuário.

4. BASE LEGAL (LGPD — Lei 13.709/2018)
O tratamento ocorre com base no:
• Art. 7º, I — Consentimento do titular (aceite nestes termos).
• Art. 7º, VI — Exercício regular de direitos em processo judicial, administrativo ou arbitral.

5. ARMAZENAMENTO
Os dados são armazenados:
• No Firebase Firestore (serviço do Google), com criptografia em trânsito (TLS) e em repouso.
• Localmente no dispositivo (SharedPreferences), apenas para configurações do aplicativo.

6. COMPARTILHAMENTO
Os dados NÃO são compartilhados com terceiros, exceto:
• Google Firebase (infraestrutura de banco de dados e autenticação).
• Destinatário de relatórios por e-mail, escolhido pelo próprio usuário.

7. DIREITOS DO TITULAR
Conforme a LGPD, você tem direito a:
• Acessar, corrigir e excluir seus dados.
• Revogar o consentimento a qualquer momento.
Para exercer seus direitos: almeidainteligencia@gmail.com

8. RETENÇÃO E EXCLUSÃO
Os dados são mantidos enquanto o usuário possuir conta ativa. Para exclusão completa, entre em contato via e-mail.

9. SEGURANÇA
Adotamos medidas técnicas como: autenticação Google OAuth 2.0, criptografia HTTPS/TLS, controle de acesso por Firebase Rules e autenticação biométrica opcional.

10. CONTATO DO DPO (Encarregado de Dados)
almeidainteligencia@gmail.com
""".trimIndent()
