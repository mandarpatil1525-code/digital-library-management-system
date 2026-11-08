package com.library.repository;

import com.library.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByUserIdOrderByIssueDateDesc(Long userId);
    List<Issue> findByReturnDateIsNullOrderByDueDateAsc();
    Optional<Issue> findByUserIdAndBookIdAndReturnDateIsNull(Long userId, Long bookId);
    long countByUserIdAndReturnDateIsNull(Long userId);
    boolean existsByBookId(Long bookId);
    boolean existsByBookIdAndReturnDateIsNull(Long bookId);
}
