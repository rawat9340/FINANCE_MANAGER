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

    @Query("SELECT c FROM Category c WHERE ((:userId IS NOT NULL AND c.user.id = :userId) OR c.isCustom = false) AND c.isDeleted = false ORDER BY c.name ASC")
    List<Category> findAllAccessibleByUserId(@Param("userId") Long userId);

    default List<Category> findAllAccessibleByUser(User user) {
        return findAllAccessibleByUserId(user != null ? user.getId() : null);
    }

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND ((:userId IS NOT NULL AND c.user.id = :userId) OR c.isCustom = false) AND c.isDeleted = false")
    Optional<Category> findAccessibleByNameIgnoreCaseAndUserId(@Param("name") String name, @Param("userId") Long userId);

    default Optional<Category> findAccessibleByNameIgnoreCase(String name, User user) {
        return findAccessibleByNameIgnoreCaseAndUserId(name, user != null ? user.getId() : null);
    }

    Optional<Category> findByNameIgnoreCaseAndUserAndIsDeletedFalse(String name, User user);

    Optional<Category> findByNameIgnoreCaseAndIsCustomFalse(String name);

    boolean existsByNameIgnoreCaseAndIsCustomFalse(String name);

    boolean existsByNameIgnoreCaseAndUserAndIsDeletedFalse(String name, User user);
}
