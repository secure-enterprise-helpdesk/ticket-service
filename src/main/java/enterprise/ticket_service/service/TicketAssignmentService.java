package enterprise.ticket_service.service;

import enterprise.ticket_service.client.user_service.dto.CreateUserProfileRequest;
import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.entity.TicketHistory;
import enterprise.ticket_service.enums.TicketStatus;
import enterprise.ticket_service.outbox.service.OutboxService;
import enterprise.ticket_service.repository.TicketHistoryRepository;
import enterprise.ticket_service.repository.TicketRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketAssignmentService {

    private final ResilientUserService resilientUserService;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final ServiceTokenService serviceTokenService;
    private final OutboxService outboxService;

    public void assignPendingTickets() {
        String accessToken = serviceTokenService.getToken();
        List<CreateUserProfileRequest> agentProfiles = resilientUserService.getAgents("Bearer " + accessToken);
        if (agentProfiles == null || agentProfiles.isEmpty()) return;
        Map<String, Long> agentWorkloadMap = computeAgentWorkload();
        List<String> unassignedTicketIds = fetchPendingTicketIds();
        if (unassignedTicketIds.isEmpty()) return;
        List<String> activeAgentIds = agentProfiles.stream()
                .map(CreateUserProfileRequest::getUserId)
                .collect(Collectors.toList());
        agentWorkloadMap.keySet().removeIf(agentId -> !activeAgentIds.contains(agentId));
        for (String agentId : activeAgentIds) {
            agentWorkloadMap.putIfAbsent(agentId, 0L);
        }
        PriorityQueue<String> leastLoadedAgents = new PriorityQueue<>(
                Comparator.comparingLong(agentId -> agentWorkloadMap.get(agentId))
        );
        leastLoadedAgents.addAll(activeAgentIds);
        List<AssignmentPair> pendingAssignments = new ArrayList<>();
        for (String ticketId : unassignedTicketIds) {
            String agentId = leastLoadedAgents.poll();
            pendingAssignments.add(new AssignmentPair(agentId, ticketId));
            agentWorkloadMap.put(
                    agentId,
                    agentWorkloadMap.get(agentId) + 1
            );
            leastLoadedAgents.offer(agentId);
        }
        assignTicket(pendingAssignments);
    }

    private Map<String, Long> computeAgentWorkload() {

        List<Object[]> results = ticketRepository.findAgentWorkloads(
                List.of(TicketStatus.CLOSED, TicketStatus.OPEN)
        );

        return results.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
        ));
    }

    private List<String> fetchPendingTicketIds() {
        return ticketRepository.findPendingTicketNumbers(TicketStatus.OPEN);
    }

    private void assignTicket(List<AssignmentPair> assignmentPairs) {
        assignmentPairs.forEach(pair -> {
            Optional<Ticket> optionalTicket =ticketRepository.findByTicketNumber(pair.ticketId);
            if (optionalTicket.isPresent()) {
                Ticket ticket = optionalTicket.get();
                ticket.setStatus(TicketStatus.ASSIGNED);
                ticket.setAssignedTo(pair.agentId);
                ticketRepository.save(ticket);
                ticketHistoryRepository.save(createHistory(pair.ticketId, "ASSIGNED", "", pair.agentId, "SYSTEM"));
                outboxService.saveTicketAssignedEvent(ticket,"SYSTEM");
            }
        });
    }

    private TicketHistory createHistory(String ticketNumber, String action, String oldValue, String newValue, String changedBy) {
        TicketHistory history = new TicketHistory();
        history.setTicketNumber(ticketNumber);
        history.setAction(action);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedBy(changedBy);
        return history;
    }
}

class AssignmentPair {
    String agentId;
    String ticketId;
    public AssignmentPair(String agentId, String ticketId) {
        this.agentId = agentId;
        this.ticketId = ticketId;
    }
}