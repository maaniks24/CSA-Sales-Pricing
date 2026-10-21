package com.levantare.pricing.repo;

import com.levantare.pricing.domain.DocSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocSequenceRepository extends JpaRepository<DocSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CsaDocSequence s where s.seqKey = :key")
    Optional<DocSequence> lockByKey(@Param("key") String key);
}
