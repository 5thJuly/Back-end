package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.entities.DifficultyLevel;
import org.example.technihongo.enums.DifficultyLevelEnum;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.repositories.DifficultyLevelRepository;
import org.example.technihongo.services.interfaces.DifficultyLevelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DifficultyLevelServiceImpl implements DifficultyLevelService {
    private final DifficultyLevelRepository difficultyLevelRepository;

    @Override
    public List<DifficultyLevel> viewAllDifficultyLevels() {
        return difficultyLevelRepository.findAll();
    }

    @Override
    public DifficultyLevel viewDifficultyLevelByTag(DifficultyLevelEnum tag) {
        DifficultyLevel difficultyLevel = difficultyLevelRepository.findByTag(tag);
        if (difficultyLevel == null) {
            throw new ResourceNotFoundException("Difficulty Level not found with tag: " + tag);
        }
        return difficultyLevel;
    }
}
