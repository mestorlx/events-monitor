package net.consensys.eventeum.integration.eventstore.db.repository;

import net.consensys.eventeum.dto.event.ContractEventDetails;
import net.consensys.eventeum.factory.EventStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("contractEventDetailRepository")
@ConditionalOnProperty(name = "eventStore.type", havingValue = "DB")
@ConditionalOnMissingBean(EventStoreFactory.class)
public interface ContractEventDetailsRepository extends MongoRepository<ContractEventDetails, String> {
    Page<ContractEventDetails> findByEventSpecificationSignature(String signature, Pageable pagination);

    List<ContractEventDetails> findByEventSpecificationSignature(String signature);
}