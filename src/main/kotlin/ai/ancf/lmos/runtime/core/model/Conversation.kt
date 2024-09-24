/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.model

import ai.ancf.lmos.arc.api.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Conversation(
    val inputContext: InputContext,
    val systemContext: SystemContext,
    val userContext: UserContext,
)

data class InputContext(val messages: List<Message>)

@Serializable
@SerialName("systemContext")
data class SystemContext(val channelId: String)

@Serializable
@SerialName("userContext")
data class UserContext(val userId: String, val userToken: String?)

sealed class ChatMessage {
    abstract val content: String
}

data class AssistantMessage(override val content: String) : ChatMessage()
