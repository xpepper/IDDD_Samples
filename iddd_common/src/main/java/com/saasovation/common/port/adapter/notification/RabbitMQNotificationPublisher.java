//   Copyright 2012,2013 Vaughn Vernon
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package com.saasovation.common.port.adapter.notification;

import com.saasovation.common.domain.model.DomainEvent;
import com.saasovation.common.event.EventStore;
import com.saasovation.common.event.StoredEvent;
import com.saasovation.common.notification.*;
import com.saasovation.common.port.adapter.messaging.rabbitmq.ConnectionSettings;
import com.saasovation.common.port.adapter.messaging.rabbitmq.Exchange;
import com.saasovation.common.port.adapter.messaging.rabbitmq.MessageParameters;
import com.saasovation.common.port.adapter.messaging.rabbitmq.MessageProducer;

import java.util.ArrayList;
import java.util.List;

public class RabbitMQNotificationPublisher implements NotificationPublisher {

    private EventStore eventStore;
    private String exchangeName;

    private PublishedNotificationTrackerStore publishedNotificationTrackerStore;

    public RabbitMQNotificationPublisher(
        EventStore anEventStore,
        PublishedNotificationTrackerStore aPublishedNotificationTrackerStore,
        Object aMessagingLocator) {

        super();

        setEventStore(anEventStore);
        setExchangeName((String) aMessagingLocator);
        setPublishedNotificationTrackerStore(aPublishedNotificationTrackerStore);
    }

    @Override
    public void publishNotifications() {
        PublishedNotificationTracker publishedNotificationTracker =
            publishedNotificationTrackerStore().publishedNotificationTracker();

        List<Notification> notifications =
            listUnpublishedNotifications(
                publishedNotificationTracker.mostRecentPublishedNotificationId());

        MessageProducer messageProducer = messageProducer();

        try {
            for (Notification notification : notifications) {
                publish(notification, messageProducer);
            }

            publishedNotificationTrackerStore()
                .trackMostRecentPublishedNotification(
                    publishedNotificationTracker,
                    notifications);
        } finally {
            messageProducer.close();
        }
    }

    @Override
    public boolean internalOnlyTestConfirmation() {
        throw new UnsupportedOperationException("Not supported by production implementation.");
    }

    private EventStore eventStore() {
        return eventStore;
    }

    private void setEventStore(EventStore anEventStore) {
        eventStore = anEventStore;
    }

    private String exchangeName() {
        return exchangeName;
    }

    private void setExchangeName(String anExchangeName) {
        exchangeName = anExchangeName;
    }

    private List<Notification> listUnpublishedNotifications(long aMostRecentPublishedMessageId) {
        List<StoredEvent> storedEvents = eventStore().allStoredEventsSince(aMostRecentPublishedMessageId);

        return notificationsFrom(storedEvents);
    }

    private MessageProducer messageProducer() {

        // creates my exchange if non-existing
        Exchange exchange =
            Exchange.fanOutInstance(
                ConnectionSettings.instance(),
                exchangeName(),
                true);

        // create a message producer used to forward events
        MessageProducer messageProducer = MessageProducer.instance(exchange);

        return messageProducer;
    }

    private List<Notification> notificationsFrom(List<StoredEvent> aStoredEvents) {
        List<Notification> notifications = new ArrayList<>(aStoredEvents.size());

        for (StoredEvent storedEvent : aStoredEvents) {
            DomainEvent domainEvent = storedEvent.toDomainEvent();

            Notification notification = new Notification(storedEvent.eventId(), domainEvent);

            notifications.add(notification);
        }

        return notifications;
    }

    private void publish(Notification aNotification, MessageProducer aMessageProducer) {

        MessageParameters messageParameters =
            MessageParameters.durableTextParameters(
                aNotification.typeName(),
                Long.toString(aNotification.notificationId()),
                aNotification.occurredOn());

        String notification =
            NotificationSerializer
                .instance()
                .serialize(aNotification);

        aMessageProducer.send(notification, messageParameters);
    }

    private PublishedNotificationTrackerStore publishedNotificationTrackerStore() {
        return publishedNotificationTrackerStore;
    }

    private void setPublishedNotificationTrackerStore(PublishedNotificationTrackerStore publishedNotificationTrackerStore) {
        this.publishedNotificationTrackerStore = publishedNotificationTrackerStore;
    }
}
