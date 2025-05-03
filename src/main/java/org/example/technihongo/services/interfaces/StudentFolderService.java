package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.StudentFolderDTO;

import java.util.List;

public interface StudentFolderService {
    StudentFolderDTO createStudentFolder(Integer studentId, StudentFolderDTO folderDTO);
    StudentFolderDTO updateStudentFolder(Integer studentId,Integer folderId, StudentFolderDTO folderDTO);
    StudentFolderDTO getStudentFolderById(Integer studentId,Integer folderId);

    void deleteStudentFolder(Integer studentId,Integer folderId);
    List<StudentFolderDTO> listAllStudentFolders(Integer studentId);
}
