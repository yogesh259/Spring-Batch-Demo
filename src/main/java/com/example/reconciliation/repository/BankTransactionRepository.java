package com.example.reconciliation.repository;

import com.example.reconciliation.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, String> {

    @Query("SELECT DISTINCT t.branch FROM BankTransaction t")
    List<String> findAllBranches();
}
