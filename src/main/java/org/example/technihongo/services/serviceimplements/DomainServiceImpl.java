package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.DomainRequestDTO;
import org.example.technihongo.dto.DomainResponseDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.entities.Domain;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.DomainService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DomainServiceImpl implements DomainService {

    private final DomainRepository domainRepository;

    private final LearningPathRepository learningPathRepository;

    private final CourseRepository courseRepository;


    @Override
    public DomainResponseDTO createDomain(DomainRequestDTO request) {
        Domain domain = new Domain();
        domain.setTag(request.getTag());
        domain.setName(request.getName());
        domain.setDescription(request.getDescription());

        if (request.getParentDomainId() != null) {
            Domain parentDomain = domainRepository.findById(request.getParentDomainId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Domain not found with ID: " + request.getParentDomainId()));
            domain.setParentDomain(parentDomain);
        } else {
            domain.setParentDomain(null);
        }

        Domain savedDomain = domainRepository.save(domain);
        return convertToDomainResponseDTO(savedDomain);
    }

    @Override
    public DomainResponseDTO updateDomain(Integer domainId, DomainRequestDTO request) {
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found with ID: " + domainId));

        if (domain.getSubDomains() != null && !domain.getSubDomains().isEmpty()) {
            throw new RuntimeException("Cannot update domain with ID: " + domainId +
                    " because it is a parent domain with existing child domains.");
        }

        if (request.getTag() != null) {
            domain.setTag(request.getTag());
        }
        if (request.getName() != null) {
            domain.setName(request.getName());
        }
        if (request.getDescription() != null) {
            domain.setDescription(request.getDescription());
        }
        if (request.getParentDomainId() != null) {
            Domain parentDomain = domainRepository.findById(request.getParentDomainId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Domain not found with ID: " + request.getParentDomainId()));
            domain.setParentDomain(parentDomain);
        }

        Domain updatedDomain = domainRepository.save(domain);
        return convertToDomainResponseDTO(updatedDomain);
    }


    @Override
    public void deleteDomain(Integer domainId) {
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found with ID: " + domainId));

        if (domain.getSubDomains() != null && !domain.getSubDomains().isEmpty()) {
            throw new RuntimeException("Cannot delete domain with ID: " + domainId +
                    " because it has associated child domains.");
        }

        if (learningPathRepository.existsByDomainDomainId(domainId)) {
            throw new RuntimeException("Cannot delete domain with ID: " + domainId +
                    " because it is referenced by one or more learning paths.");
        }

        if (courseRepository.existsByDomainDomainId(domainId)) {
            throw new RuntimeException("Cannot delete domain with ID: " + domainId +
                    " because it is referenced by one or more courses.");
        }
        domainRepository.delete(domain);
    }

    @Override
    public PageResponseDTO<DomainResponseDTO> getAllDomains(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Domain> domainPage = domainRepository.findAll(pageable);
        List<DomainResponseDTO> content = domainPage.getContent().stream()
                .map(this::convertToDomainResponseDTO)
                .collect(Collectors.toList());

        return buildPageResponseDTO(domainPage, content, pageNo, pageSize);
    }

    @Override
    public PageResponseDTO<DomainResponseDTO> getAllParentDomains(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Domain> parentDomainPage = domainRepository.findByParentDomainIsNull(pageable);
        List<DomainResponseDTO> content = parentDomainPage.getContent().stream()
                .map(this::convertToDomainResponseDTO)
                .collect(Collectors.toList());

        return buildPageResponseDTO(parentDomainPage, content, pageNo, pageSize);
    }

    @Override
    public PageResponseDTO<DomainResponseDTO> getAllChildrenDomains(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Domain> childrenDomainPage = domainRepository.findByParentDomainIsNotNull(pageable);
        List<DomainResponseDTO> content = childrenDomainPage.getContent().stream()
                .map(this::convertToDomainResponseDTO)
                .collect(Collectors.toList());

        return buildPageResponseDTO(childrenDomainPage, content, pageNo, pageSize);
    }

    @Override
    public DomainResponseDTO getDomainById(Integer domainId) {
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found with ID: " + domainId));
        return convertToDomainResponseDTO(domain);
    }

    @Override
    public PageResponseDTO<DomainResponseDTO> searchName(String keyword, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Domain> domainPage;
        if (keyword == null || keyword.trim().isEmpty()) {
            domainPage = Page.empty(pageable);
        } else {
            domainPage = domainRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }

        List<DomainResponseDTO> content = domainPage.getContent().stream()
                .map(this::convertToDomainResponseDTO)
                .collect(Collectors.toList());

        return buildPageResponseDTO(domainPage, content, pageNo, pageSize);
    }

    @Override
    public PageResponseDTO<DomainResponseDTO> getDomainsByTags(List<String> tags, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Domain> domainPage;
        if (tags == null || tags.isEmpty()) {
            domainPage = Page.empty(pageable);
        } else {
            domainPage = domainRepository.findByTagIn(tags, pageable);
        }

        List<DomainResponseDTO> content = domainPage.getContent().stream()
                .map(this::convertToDomainResponseDTO)
                .collect(Collectors.toList());

        return buildPageResponseDTO(domainPage, content, pageNo, pageSize);
    }

    @Override
    public PageResponseDTO<DomainResponseDTO> getChildDomains(Integer parentDomainId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Domain parentDomain = domainRepository.findById(parentDomainId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent Domain not found with ID: " + parentDomainId));

        Page<Domain> childDomainPage = domainRepository.findByParentDomain(parentDomain, pageable);
        List<DomainResponseDTO> content = childDomainPage.getContent().stream()
                .map(this::convertToDomainResponseDTO)
                .collect(Collectors.toList());

        return buildPageResponseDTO(childDomainPage, content, pageNo, pageSize);
    }

    private DomainResponseDTO convertToDomainResponseDTO(Domain domain) {
        DomainResponseDTO response = new DomainResponseDTO();
        response.setDomainId(domain.getDomainId());
        response.setTag(domain.getTag());
        response.setName(domain.getName());
        response.setDescription(domain.getDescription());
        response.setParentDomainId(domain.getParentDomain() != null ? domain.getParentDomain().getDomainId() : null);
        return response;
    }

    private PageResponseDTO<DomainResponseDTO> buildPageResponseDTO(Page<Domain> page, List<DomainResponseDTO> content, int pageNo, int pageSize) {
        return PageResponseDTO.<DomainResponseDTO>builder()
                .content(content)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}