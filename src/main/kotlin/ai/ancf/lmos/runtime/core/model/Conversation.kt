/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.model

import ai.ancf.lmos.arc.api.AnonymizationEntity
import ai.ancf.lmos.arc.api.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Conversation(
    val inputContext: InputContext,
    val systemContext: SystemContext,
    val userContext: UserContext,
)

data class InputContext(
    val messages: List<Message>,
    val explicitAgent: String? = null,
    val anonymizationEntities: List<AnonymizationEntity>? = null,
)

@Serializable
@SerialName("systemContext")
data class SystemContext(
    val channelId: String,
    val contextParams: Map<String, String> = mapOf(),
)

@Serializable
@SerialName("userContext")
data class UserContext(
    val userId: String,
    val userToken: String?,
    val contextParams: Map<String, String> = mapOf(),
)

sealed class ChatMessage {
    abstract val content: String
}

data class AssistantMessage(
    override val content: String,
    val anonymizationEntities: List<AnonymizationEntity>? = emptyList(),
) : ChatMessage()
