package com.example.reconciliation.repository;

import com.example.reconciliation.domain.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, String> {

    @Query("SELECT r.status AS status, COUNT(r) AS total "
            + "FROM ReconciliationResult r GROUP BY r.status ORDER BY r.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        String getStatus();

        long getTotal();
    }
}
