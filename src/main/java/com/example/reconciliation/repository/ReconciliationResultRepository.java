package com.example.reconciliation.repository;

import com.example.reconciliation.domain.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, String> {

    /** Used by the summary step. The database does the counting. */
    @Query("SELECT r.status AS status, COUNT(r) AS total "
            + "FROM ReconciliationResult r GROUP BY r.status ORDER BY r.status")
    List<StatusCount> countByStatus();

    /** Spring Data projection: just the two columns the query returns. */
    interface StatusCount {
        String getStatus();

        long getTotal();
    }
}
