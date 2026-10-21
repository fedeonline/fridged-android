package io.github.fedeonline.fridged.ui.signin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.auth.SignInResult
import io.github.fedeonline.fridged.ui.theme.FridgedIcons
import io.github.fedeonline.fridged.ui.theme.FridgedTheme

@Composable
fun SignInScreen(viewModel: SignInViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SignInContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::signIn,
    )
}

@Composable
private fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    // Purely visual, so it lives here and not in the view model. Starts hidden.
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.sign_in_email)) },
                singleLine = true,
                enabled = !state.submitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.EmailAddress + ContentType.Username },
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.sign_in_password)) },
                singleLine = true,
                enabled = !state.submitting,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) FridgedIcons.EyeOff else FridgedIcons.Eye,
                            contentDescription = stringResource(
                                if (showPassword) R.string.sign_in_hide_password else R.string.sign_in_show_password,
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password },
            )
            state.error?.let {
                Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onSubmit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
                if (state.submitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                } else {
                    Text(stringResource(R.string.sign_in_button))
                }
            }
        }
    }
}

@StringRes
private fun SignInResult.messageRes(): Int = when (this) {
    SignInResult.INVALID_CREDENTIALS -> R.string.sign_in_error_invalid_credentials
    SignInResult.NOT_AUTHORIZED -> R.string.sign_in_error_not_authorized
    SignInResult.TOO_MANY_ATTEMPTS -> R.string.sign_in_error_too_many_attempts
    SignInResult.NETWORK_ERROR -> R.string.sign_in_error_network
    SignInResult.SUCCESS, SignInResult.UNKNOWN_ERROR -> R.string.sign_in_error_unknown
}

@Preview
@Composable
private fun SignInPreview() {
    FridgedTheme {
        SignInContent(
            state = SignInUiState(email = "me@example.com", error = SignInResult.INVALID_CREDENTIALS),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}
