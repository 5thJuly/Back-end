package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.MeetingScriptDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.ScriptOrderDTO;
import org.example.technihongo.entities.MeetingScript;

public interface MeetingScriptService {
    PageResponseDTO<MeetingScript> getListScriptsByMeetingId(Integer meetingId, int pageNo, int pageSize, String sortBy, String sortDir);
    MeetingScript getScriptById(Integer scriptId);
    MeetingScript createScript(MeetingScriptDTO dto);
    void updateScript(Integer scriptId, MeetingScriptDTO dto);
    void deleteScript(Integer scriptId);
    void updateScriptOrder(Integer meetingId, ScriptOrderDTO scriptOrderDTO);
}
