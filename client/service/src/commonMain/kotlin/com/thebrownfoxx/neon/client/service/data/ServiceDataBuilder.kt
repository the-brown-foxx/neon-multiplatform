package com.thebrownfoxx.neon.client.service.data

import com.thebrownfoxx.neon.client.service.data.model.GroupRecord
import com.thebrownfoxx.neon.client.service.data.model.MemberRecord
import com.thebrownfoxx.neon.client.service.data.model.ServiceData
import com.thebrownfoxx.neon.common.model.GroupId
import com.thebrownfoxx.neon.common.model.Message
import com.thebrownfoxx.neon.common.type.Url

typealias ServiceDataBuilder = ServiceDataBuilderScope.() -> Unit

fun serviceData(builder: ServiceDataBuilder): ServiceData {
    val builderScope = ServiceDataBuilderScope().apply { builder() }
    return builderScope.build()
}

class ServiceDataBuilderScope internal constructor() {
    private val groups = mutableListOf<GroupRecord>()
    private val members = mutableListOf<MemberRecord>()
    private val messages = mutableListOf<Message>()

    fun community(
        name: String,
        avatarUrl: Url?,
        inviteCode: String,
        god: Boolean = false,
        builder: CommunityBuilder = {},
    ): GroupId {
        val communityBuilderScope = CommunityBuilderScope(
            name = name,
            avatarUrl = avatarUrl,
            inviteCode = inviteCode,
            god = god,
        ).apply(builder)

        val communityBuilderData = communityBuilderScope.build()

        groups.add(communityBuilderData.communityRecord)
        members.addAll(communityBuilderData.memberRecords)

        return communityBuilderData.communityRecord.group.id
    }

    fun conversation(builder: DirectConversationBuilder): GroupId {
        val directConversationRecord = DirectConversationBuilderScope().apply(builder).build()
        groups.add(directConversationRecord.chatGroupRecord)
        messages.addAll(directConversationRecord.messages)
        return directConversationRecord.chatGroupRecord.group.id
    }

    fun GroupId.conversation(builder: ConversationBuilder) {
        val conversationBuilderScope = ConversationBuilderScope(this).apply(builder)
        messages.addAll(conversationBuilderScope.build())
    }

    internal fun build() = ServiceData(members, groups, messages)
}