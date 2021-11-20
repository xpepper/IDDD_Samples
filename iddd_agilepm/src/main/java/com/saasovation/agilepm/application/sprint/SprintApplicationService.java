package com.saasovation.agilepm.application.sprint;

import com.saasovation.agilepm.domain.model.product.backlogitem.BacklogItem;
import com.saasovation.agilepm.domain.model.product.backlogitem.BacklogItemId;
import com.saasovation.agilepm.domain.model.product.backlogitem.BacklogItemRepository;
import com.saasovation.agilepm.domain.model.product.sprint.Sprint;
import com.saasovation.agilepm.domain.model.product.sprint.SprintId;
import com.saasovation.agilepm.domain.model.product.sprint.SprintRepository;
import com.saasovation.agilepm.domain.model.tenant.TenantId;

public class SprintApplicationService {

    private final BacklogItemRepository backlogItemRepository;
    private final SprintRepository sprintRepository;

    public SprintApplicationService(
            SprintRepository aSprintRepository,
            BacklogItemRepository aBacklogItemRepository) {

        super();

        this.backlogItemRepository = aBacklogItemRepository;
        this.sprintRepository = aSprintRepository;
    }

    public void commitBacklogItemToSprint(CommitBacklogItemToSprintCommand aCommand) {

        TenantId tenantId = new TenantId(aCommand.getTenantId());

        Sprint sprint =
                this.sprintRepository()
                        .sprintOfId(
                                tenantId,
                                new SprintId(aCommand.getSprintId()));

        BacklogItem backlogItem =
                this.backlogItemRepository()
                        .backlogItemOfId(
                                tenantId,
                                new BacklogItemId(aCommand.getBacklogItemId()));

        sprint.commit(backlogItem);

        this.sprintRepository().save(sprint);
    }

    private BacklogItemRepository backlogItemRepository() {
        return this.backlogItemRepository;
    }

    private SprintRepository sprintRepository() {
        return this.sprintRepository;
    }
}
