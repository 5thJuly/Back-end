package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.FolderItemDTO;
import org.example.technihongo.dto.RemoveItemDTO;
import org.example.technihongo.entities.FolderItem;
import org.example.technihongo.entities.StudentFlashcardSet;
import org.example.technihongo.entities.StudentFolder;
import org.example.technihongo.repositories.FolderItemRepository;
import org.example.technihongo.repositories.StudentFlashcardSetRepository;
import org.example.technihongo.repositories.StudentFolderRepository;
import org.example.technihongo.services.interfaces.FolderItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FolderItemServiceImpl implements FolderItemService {
    private final FolderItemRepository folderItemRepository;
    private final StudentFolderRepository studentFolderRepository;
    private final StudentFlashcardSetRepository studentFlashcardSetRepository;

    @Override
    public FolderItemDTO addFolderItem(Integer studentId, FolderItemDTO folderItemDTO) {
        StudentFolder studentFolder = studentFolderRepository.findById(folderItemDTO.getFolderId())
                .orElseThrow(() -> new RuntimeException("Student folder not found with ID: " + folderItemDTO.getFolderId()));

        if (!studentFolder.getStudent().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to add items to this folder");
        }

        if (studentFolder.isDeleted()) {
            throw new IllegalStateException("Cannot add item to a deleted folder");
        }
        StudentFlashcardSet studentFlashcardSet = studentFlashcardSetRepository.findById(folderItemDTO.getStudentSetId())
                .orElseThrow(() -> new RuntimeException("Student flashcard set not found with ID: " + folderItemDTO.getStudentSetId()));

        if (!studentFlashcardSet.getCreator().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to access this flashcard set");
        }

        Optional<FolderItem> existingItem = folderItemRepository.findByStudentFolderFolderIdAndStudentFlashcardSetStudentSetId(
                folderItemDTO.getFolderId(),
                folderItemDTO.getStudentSetId());

        if (existingItem.isPresent()) {
            throw new IllegalStateException("This flashcard set is already in the folder");
        }

        FolderItem folderItem = FolderItem.builder()
                .studentFolder(studentFolder)
                .studentFlashcardSet(studentFlashcardSet)
                .build();

        FolderItem savedFolderItem = folderItemRepository.save(folderItem);

        return convertToDTO(savedFolderItem);
    }

    @Override
    public List<FolderItemDTO> addMultipleFolderItems(Integer studentId, List<FolderItemDTO> folderItemDTOs) {
        List<FolderItemDTO> results = new ArrayList<>();
        for (FolderItemDTO dto : folderItemDTOs) {
            try {
                FolderItemDTO addItem = addFolderItem(studentId, dto);
                results.add(addItem);
            } catch (Exception e) {
                System.err.println("Failed to add folder item: " + e.getMessage());
            }
        }
        return results;
    }

    @Override
    public void removeFolderItem(Integer studentId, RemoveItemDTO removeItemDTO) {
        Integer folderItemId = removeItemDTO.getFolderItemId();

        FolderItem folderItem = folderItemRepository.findById(folderItemId)
                .orElseThrow(() -> new RuntimeException("Folder item not found with ID: " + folderItemId));

        if (!folderItem.getStudentFolder().getStudent().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to remove items from this folder");
        }

        folderItemRepository.deleteById(folderItemId);
    }

    @Override
    public void removeMultipleFolderItems(Integer studentId, List<RemoveItemDTO> requests) {
        for (RemoveItemDTO request : requests) {
            try {
                removeFolderItem(studentId, request);
            } catch (Exception e) {
                System.err.println("Failed to remove folder item: " + e.getMessage());
            }
        }
    }

    @Override
    public List<FolderItemDTO> getFolderItemsByFolderId(Integer studentId, Integer folderId) {
        if (folderId == null) {
            throw new IllegalArgumentException("Folder ID cannot be null");
        }

        StudentFolder folder = studentFolderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Student folder not found with ID: " + folderId));
        if (!folder.getStudent().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to access this folder");
        }
        if (folder.isDeleted()) {
            throw new IllegalStateException("Cannot get items from a deleted folder");
        }
        List<FolderItem> folderItems = folderItemRepository.findByStudentFolderFolderId(folderId);

        return folderItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FolderItemDTO> searchItems(Integer studentId, Integer folderId, String searchTerm) {
        StudentFolder folder = studentFolderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Student folder not found with ID: " + folderId));

        if (!folder.getStudent().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to access this folder");
        }

        if (folder.isDeleted()) {
            throw new IllegalStateException("Cannot search items in a deleted folder");
        }

        List<FolderItem> folderItems = folderItemRepository.findByStudentFolderFolderId(folderId);

        return folderItems.stream()
                .filter(item -> item.getStudentFlashcardSet().getTitle().toLowerCase()
                        .contains(searchTerm.toLowerCase()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FolderItemDTO moveItem(Integer studentId, Integer folderItemId, Integer targetFolderId) {
        FolderItem folderItem = folderItemRepository.findById(folderItemId)
                .orElseThrow(() -> new RuntimeException("Folder item not found with ID: " + folderItemId));

        StudentFolder sourceFolder = folderItem.getStudentFolder();
        if (!sourceFolder.getStudent().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to move items from this folder");
        }

        StudentFolder targetFolder = studentFolderRepository.findById(targetFolderId)
                .orElseThrow(() -> new RuntimeException("Target folder not found with ID: " + targetFolderId));

        if (!targetFolder.getStudent().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to move items to this folder");
        }

        if (sourceFolder.isDeleted() || targetFolder.isDeleted()) {
            throw new IllegalStateException("Cannot move items from/to a deleted folder");
        }

        Optional<FolderItem> existingItem = folderItemRepository
                .findByStudentFolderFolderIdAndStudentFlashcardSetStudentSetId(
                        targetFolderId,
                        folderItem.getStudentFlashcardSet().getStudentSetId());

        if (existingItem.isPresent()) {
            throw new IllegalStateException("This flashcard set is already in the target folder");
        }

        folderItem.setStudentFolder(targetFolder);
        FolderItem updatedItem = folderItemRepository.save(folderItem);

        return convertToDTO(updatedItem);
    }

    @Override
    public int countFolderItemsInFolder(Integer studentId, Integer folderId) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        if (folderId == null) {
            throw new IllegalArgumentException("Folder ID cannot be null");
        }
        StudentFolder folder = studentFolderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Student folder not found with ID: " + folderId));

        if (!folder.getStudent().getStudentId().equals(studentId)) {
            throw new SecurityException("You do not have permission to access this folder");
        }
        if (folder.isDeleted()) {
            throw new IllegalStateException("Cannot count items in a deleted folder");
        }
        long count = folderItemRepository.countByStudentFolderFolderId(folderId);
        return (int) count;
    }

    private FolderItemDTO convertToDTO(FolderItem item) {
        return new FolderItemDTO(
                item.getFolderItemId(),
                item.getStudentFolder().getFolderId(),
                item.getStudentFlashcardSet().getStudentSetId()
        );
    }
}