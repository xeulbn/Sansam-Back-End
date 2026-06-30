package org.example.sansam.s3.repository;

import org.example.sansam.s3.domain.FileDetail;
import org.example.sansam.s3.domain.FileManagement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileDetailJpaRepository extends JpaRepository<FileDetail, Long> {
    List<FileDetail> findByFileManagement(FileManagement fileManagement);

    @Query("""
    select fd
    from FileDetail fd
    where fd.fileManagement.id in :fileManagementIds
    order by fd.fileManagement.id asc, fd.isMain desc, fd.id asc
""")
    List<FileDetail> findAllByFileManagementIdIn(@Param("fileManagementIds") List<Long> fileManagementIds);
}
