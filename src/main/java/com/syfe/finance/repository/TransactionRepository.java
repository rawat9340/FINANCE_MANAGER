package com.syfe.finance.repository;

import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.Transaction;
import com.syfe.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserAndIsDeletedFalse(Long id, User user);

    boolean existsByCategoryAndIsDeletedFalse(Category category);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.user = :user AND t.date >= :startDate AND t.date <= :endDate AND t.isDeleted = false")
    List<Transaction> findByUserAndDateBetweenAndIsDeletedFalse(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.date >= :startDate AND t.isDeleted = false")
    List<Transaction> findByUserAndDateAfterOrEqualAndIsDeletedFalse(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate
    );
}
