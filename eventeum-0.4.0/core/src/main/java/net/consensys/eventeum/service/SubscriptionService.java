package net.consensys.eventeum.service;

import net.consensys.eventeum.dto.event.filter.ContractEventFilter;
import net.consensys.eventeum.dto.message.ContractEvent;

/**
 * A service for manageing contract event subscriptions within the Eventeum instance.
 *
 * @author Craig Williams <craig.williams@consensys.net>
 */
public interface SubscriptionService {

    /**
     * Registers a new contract event filter.
     *
     * If the id is null, then one is assigned.
     *
     * Broadcasts the added filter event to any other Eventeum instances.
     *
     * @param filter The filter to add.
     * @return The registered contract event filter
     */
    ContractEventFilter registerContractEventFilter(ContractEventFilter filter);

    /**
     * Registers a new contract event filter.
     *
     * If the id is null, then one is assigned.
     *
     * @param filter The filter to add.
     * @param broadcast Specifies if the added filter event should be broadcast to other Eventeum instances.
     * @return The registered contract event filter
     */
    ContractEventFilter registerContractEventFilter(ContractEventFilter filter, boolean broadcast);

    /**
     * Unregisters a previously added contract event filter.
     *
     * Broadcasts the removed filter event to any other Eventeum instances.
     *
     * @param filterId The filter id of the event to remove.
     */
    void unregisterContractEventFilter(String filterId) throws FilterNotFoundException ;

    /**
     * Unregisters a previously added contract event filter.
     *
     * @param filterId The filter id of the event to remove.
     * @param broadcast Specifies if the removed filter event should be broadcast to other Eventeum instances.
     */
    void unregisterContractEventFilter(String filterId, boolean broadcast) throws FilterNotFoundException;

    /**
     * Resubscribe to all currently active event filters.
     *
     * @param unsubscribeFirst Specifies if all active subscriptions should be unsubscribed first, before resubscribing.
     */
    void resubscribeToAllSubscriptions(boolean unsubscribeFirst);
}
