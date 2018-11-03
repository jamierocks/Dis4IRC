/*
 * This file is part of Dis4IRC.
 *
 * Dis4IRC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dis4IRC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Dis4IRC.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.zachbr.dis4irc.bridge.pier.discord

import io.zachbr.dis4irc.bridge.message.Message
import io.zachbr.dis4irc.bridge.message.Sender
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * Responsible for listening to incoming discord messages and filtering garbage
 */
class DiscordMsgListener(private val pier: DiscordPier) : ListenerAdapter() {
    private val logger = pier.logger

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent?) {
        if (event == null) {
            logger.debug("Null Discord message event from JDA")
            return
        }

        // dont bridge itself
        val source = event.channel.asBridgeSource()
        if (pier.isThisBot(source, event.author.idLong)) {
            return
        }

        // don't bridge empty messages (discord does this on join)
        if (event.message.contentDisplay.isEmpty()) {
            return
        }

        val receiveTimestamp = System.nanoTime()
        logger.debug("DISCORD MSG ${event.channel?.name} ${event.author.name}: ${event.message.contentStripped}")

        // We need to get the guild member in order to grab their display name
        val guildMember = event.guild.getMember(event.author)
        if (guildMember == null && !event.author.isBot) {
            logger.debug("Cannot get Discord guild member from user information: ${event.author}!")
        }

        // handle attachments
        val attachmentUrls = ArrayList<String>()
        for (attachment in event.message.attachments) {
            var url = attachment.url
            if (attachment.isImage) {
                url = attachment.proxyUrl
            }

            attachmentUrls.add(url)
        }

        // handle custom emotes
        var messageText = event.message.contentDisplay

        for (emote in event.message.emotes) {
            messageText = messageText.replace(":${emote.name}:", "")
            attachmentUrls.add(emote.imageUrl)
        }

        val displayName = guildMember?.effectiveName ?: event.author.name
        val sender = Sender(displayName, event.author.idLong, null)
        val message = Message(messageText, sender, source, receiveTimestamp, attachmentUrls)
        pier.sendToBridge(message)
    }
}
