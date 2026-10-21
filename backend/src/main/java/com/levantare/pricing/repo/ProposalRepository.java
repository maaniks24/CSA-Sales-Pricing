package com.levantare.pricing.repo;

import com.levantare.pricing.domain.Proposal;
import com.levantare.pricing.domain.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findAllByOrderByIdDesc();

    List<Proposal> findTop8ByOrderByIdDesc();

    boolean existsByCustomerId(Long customerId);

    @Query("select p.status, count(p), coalesce(sum(p.grandTotal), 0) from CsaProposal p group by p.status")
    List<Object[]> totalsByStatus();

    @Query("select p.customer.name, count(p), coalesce(sum(p.grandTotal), 0) from CsaProposal p "
            + "group by p.customer.name order by sum(p.grandTotal) desc")
    List<Object[]> totalsByCustomer();

    @Query("select p.proposalDate, p.grandTotal from CsaProposal p where p.proposalDate >= :from")
    List<Object[]> dateAndTotalSince(@Param("from") LocalDate from);

    @Query("select count(p) from CsaProposal p where p.status = :s")
    long countByStatus(@Param("s") ProposalStatus s);
}
