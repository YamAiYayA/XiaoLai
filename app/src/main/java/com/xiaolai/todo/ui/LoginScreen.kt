package com.xiaolai.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    loading: Boolean,
    message: String,
    onLoginToken: (String) -> Unit,
    onLoginInvite: (String, String) -> Unit,
) {
    var token by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("46XSCU") }
    var name by rememberSaveable { mutableStateOf("爸爸") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("宝宝记录", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "小程序数据导入服务器后，用口令进入。爸爸/妈妈各有一个口令。",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("登录口令 token") },
            shape = RoundedCornerShape(14.dp),
        )
        Button(
            onClick = { onLoginToken(token) },
            enabled = !loading && token.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (loading) "登录中…" else "用口令登录")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("或用邀请码 + 称呼", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = invite,
            onValueChange = { invite = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("邀请码") },
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("称呼（爸爸 / 妈妈 / 胡月嫂）") },
            shape = RoundedCornerShape(14.dp),
        )
        Button(
            onClick = { onLoginInvite(invite, name) },
            enabled = !loading && invite.isNotBlank() && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("用邀请码登录")
        }

        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}
