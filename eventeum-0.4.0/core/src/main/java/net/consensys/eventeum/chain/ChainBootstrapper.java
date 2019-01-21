package net.consensys.eventeum.chain;

import net.consensys.eventeum.dto.event.filter.ContractEventFilter;
import net.consensys.eventeum.factory.ContractEventFilterFactory;
import net.consensys.eventeum.service.SubscriptionService;
import net.consensys.eventeum.chain.config.EventFilterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Registers filters that are either configured within the properties file, exist in the
 * Eventeum database on startup, or are returned from ContractEventFilterFactory beans.
 *
 * @author Craig Williams <craig.williams@consensys.net>
 */
@Service
public class ChainBootstrapper implements InitializingBean {
    private final Logger LOG = LoggerFactory.getLogger(ChainBootstrapper.class);

    private SubscriptionService subscriptionService;
    private EventFilterConfiguration filterConfiguration;
    private CrudRepository<ContractEventFilter, String> filterRepository;
    private Optional<List<ContractEventFilterFactory>> contractEventFilterFactories;

    @Autowired
    public ChainBootstrapper(EventFilterConfiguration filterConfiguration,
                             SubscriptionService subscriptionService,
                             CrudRepository<ContractEventFilter, String> filterRepository,
                             Optional<List<ContractEventFilterFactory>> contractEventFilterFactories) {
        this.filterConfiguration = filterConfiguration;
        this.subscriptionService = subscriptionService;
        this.filterRepository = filterRepository;
        this.contractEventFilterFactories = contractEventFilterFactories;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        registerFilters(filterConfiguration.getConfiguredEventFilters(), true);
        registerFilters(filterRepository.findAll(), false);

        contractEventFilterFactories.ifPresent((factories) -> {
            factories.forEach(factory -> registerFilters(factory.build(), true));
        });
    }

    private void registerFilters(Iterable<ContractEventFilter> filters, boolean broadcast) {
        if (filters != null) {
            filters.forEach(filter -> registerFilter(filter, broadcast));
        }
    }

    private void registerFilter(ContractEventFilter filter, boolean broadcast) {
        subscriptionService.registerContractEventFilter(filter, broadcast);
    }
}

