package com.syfe.finance.repository;

import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE (c.user = :user OR c.isCustom = false) AND c.isDeleted = false ORDER BY c.name ASC")
    List<Category> findAllAccessibleByUser(@Param("user") User user);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.user = :user OR c.isCustom = false) AND c.isDeleted = false")
    Optional<Category> findAccessibleByNameIgnoreCase(@Param("name") String name, @Param("user") User user);

    Optional<Category> findByNameIgnoreCaseAndUserAndIsDeletedFalse(String name, User user);

    Optional<Category> findByNameIgnoreCaseAndIsCustomFalse(String name);

    boolean existsByNameIgnoreCaseAndIsCustomFalse(String name);

    boolean existsByNameIgnoreCaseAndUserAndIsDeletedFalse(String name, User user);
}
