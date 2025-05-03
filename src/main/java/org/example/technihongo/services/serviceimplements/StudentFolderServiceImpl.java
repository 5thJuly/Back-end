package org.example.technihongo.services.serviceimplements;

import org.example.technihongo.dto.FolderItemDTO;
import org.example.technihongo.dto.StudentFolderDTO;
import org.example.technihongo.entities.Student;
import org.example.technihongo.entities.StudentFolder;
import org.example.technihongo.repositories.FolderItemRepository;
import org.example.technihongo.repositories.StudentFolderRepository;
import org.example.technihongo.repositories.StudentRepository;
import org.example.technihongo.services.interfaces.FolderItemService;
import org.example.technihongo.services.interfaces.StudentFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentFolderServiceImpl implements StudentFolderService {

    @Autowired
    private StudentFolderRepository studentFolderRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FolderItemRepository folderItemRepository;

    @Autowired
    private FolderItemService folderItemService;

    @Override
    public StudentFolderDTO createStudentFolder(Integer studentId,StudentFolderDTO folderDTO) {

        if (folderDTO.getName() == null || folderDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập Folder name nhé");
        }

        if (studentId == null) {
            throw new IllegalArgumentException("Student ID is required");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        StudentFolder folder = StudentFolder.builder()
                .student(student)
                .name(folderDTO.getName())
                .description(folderDTO.getDescription())
                .build();

        folder = studentFolderRepository.save(folder);

        return convertToDTO(folder);
    }

    @Override
    public StudentFolderDTO updateStudentFolder(Integer studentId,Integer folderId, StudentFolderDTO folderDTO) {
        if (folderId == null) {
            throw new IllegalArgumentException("Folder ID cannot be null");
        }

        StudentFolder folder = studentFolderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found with ID: " + folderId));

        if (folderDTO.getName() != null && !folderDTO.getName().trim().isEmpty()) {
            folder.setName(folderDTO.getName());
        }

        if (folderDTO.getDescription() != null) {
            folder.setDescription(folderDTO.getDescription());
        }

        if (studentId != null &&
                (folder.getStudent() == null || studentId.equals(folder.getStudent().getStudentId()))) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
            folder.setStudent(student);
        }

        folder = studentFolderRepository.save(folder);
        return convertToDTO(folder);
    }

    @Override
    public StudentFolderDTO getStudentFolderById(Integer studentId, Integer folderId) {
        return studentFolderRepository.findById(folderId)
                .filter(folder -> folder.getStudent().getStudentId().equals(studentId))
                .map(this::convertToDTO)
                .orElseThrow(() -> new SecurityException("You do not have permission to access this folder or it does not exist"));
    }

    @Override
    public void deleteStudentFolder(Integer studentId, Integer folderId) {
        if (folderId == null) {
            throw new IllegalArgumentException("Folder ID cannot be null");
        }

        StudentFolder folder = studentFolderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found with ID: " + folderId));

        int itemCount = folderItemService.countFolderItemsInFolder(folder.getStudent().getStudentId(), folderId);
        if (itemCount > 0) {
            throw new IllegalStateException("Cannot delete folder with ID: " + folderId + " because it contains " + itemCount + " item(s)");
        }

        folder.setDeleted(true);
        studentFolderRepository.save(folder);
    }

    @Override
    public List<StudentFolderDTO> listAllStudentFolders(Integer studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found with ID: " + studentId);
        }

        List<StudentFolder> folders = studentFolderRepository.findByStudentStudentId(studentId);
        return folders.stream()
                .filter(folder -> !folder.isDeleted())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private StudentFolderDTO convertToDTO(StudentFolder folder) {
        StudentFolderDTO dto = new StudentFolderDTO();
        dto.setFolderId(folder.getFolderId());
        dto.setName(folder.getName());
        dto.setDescription(folder.getDescription());
        return dto;
    }
}