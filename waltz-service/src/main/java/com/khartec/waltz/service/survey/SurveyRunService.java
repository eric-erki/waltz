/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific
 *
 */

package com.khartec.waltz.service.survey;

import com.khartec.waltz.common.ListUtilities;
import com.khartec.waltz.common.SetUtilities;
import com.khartec.waltz.data.GenericSelector;
import com.khartec.waltz.data.GenericSelectorFactory;
import com.khartec.waltz.data.involvement.InvolvementDao;
import com.khartec.waltz.data.person.PersonDao;
import com.khartec.waltz.data.survey.SurveyInstanceDao;
import com.khartec.waltz.data.survey.SurveyInstanceRecipientDao;
import com.khartec.waltz.data.survey.SurveyRunDao;
import com.khartec.waltz.data.survey.SurveyTemplateDao;
import com.khartec.waltz.model.*;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.model.person.Person;
import com.khartec.waltz.model.survey.*;
import com.khartec.waltz.service.changelog.ChangeLogService;
import org.jooq.Record1;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.common.Checks.checkTrue;
import static com.khartec.waltz.common.ListUtilities.map;
import static com.khartec.waltz.common.MapUtilities.indexBy;
import static com.khartec.waltz.common.SetUtilities.fromCollection;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
public class SurveyRunService {

    private final ChangeLogService changeLogService;
    private final InvolvementDao involvementDao;
    private final PersonDao personDao;
    private final SurveyInstanceDao surveyInstanceDao;
    private final SurveyInstanceRecipientDao surveyInstanceRecipientDao;
    private final SurveyRunDao surveyRunDao;
    private final SurveyTemplateDao surveyTemplateDao;

    private final GenericSelectorFactory genericSelectorFactory = new GenericSelectorFactory();
    private final SurveyInstanceIdSelectorFactory surveyInstanceIdSelectorFactory = new SurveyInstanceIdSelectorFactory();


    @Autowired
    public SurveyRunService(ChangeLogService changeLogService,
                            InvolvementDao involvementDao,
                            PersonDao personDao,
                            SurveyInstanceDao surveyInstanceDao,
                            SurveyInstanceRecipientDao surveyInstanceRecipientDao,
                            SurveyRunDao surveyRunDao,
                            SurveyTemplateDao surveyTemplateDao) {
        checkNotNull(changeLogService, "changeLogService cannot be null");
        checkNotNull(involvementDao, "involvementDao cannot be null");
        checkNotNull(personDao, "personDao cannot be null");
        checkNotNull(surveyInstanceDao, "surveyInstanceDao cannot be null");
        checkNotNull(surveyInstanceRecipientDao, "surveyInstanceRecipientDao cannot be null");
        checkNotNull(surveyRunDao, "surveyRunDao cannot be null");
        checkNotNull(surveyTemplateDao, "surveyTemplateDao cannot be null");

        this.changeLogService = changeLogService;
        this.involvementDao = involvementDao;
        this.personDao = personDao;
        this.surveyInstanceDao = surveyInstanceDao;
        this.surveyInstanceRecipientDao = surveyInstanceRecipientDao;
        this.surveyRunDao = surveyRunDao;
        this.surveyTemplateDao = surveyTemplateDao;
    }


    public SurveyRun getById(long id) {
        return surveyRunDao.getById(id);
    }


    public List<SurveyRun> findForRecipient(String userName) {
        checkNotNull(userName, "userName cannot be null");

        Person person = personDao.getActiveByUserEmail(userName);
        checkNotNull(person, "userName " + userName + " cannot be resolved");

        return surveyRunDao.findForRecipient(person.id().get());
    }


    public IdCommandResponse createSurveyRun(String userName, SurveyRunCreateCommand command) {
        checkNotNull(userName, "userName cannot be null");
        checkNotNull(command, "create command cannot be null");

        Person owner = personDao.getActiveByUserEmail(userName);
        checkNotNull(owner, "userName " + userName + " cannot be resolved");

        long surveyRunId = surveyRunDao.create(owner.id().get(), command);

        changeLogService.write(
                ImmutableChangeLog.builder()
                        .operation(Operation.ADD)
                        .userId(userName)
                        .parentReference(EntityReference.mkRef(EntityKind.SURVEY_RUN, surveyRunId))
                        .message("Survey Run: " + command.name() + " added")
                        .build());

        return ImmutableIdCommandResponse.builder()
                .id(surveyRunId)
                .build();
    }


    public int updateSurveyRun(String userName, long surveyRunId, SurveyRunChangeCommand command) {
        checkNotNull(userName, "userName cannot be null");
        checkNotNull(command, "change command cannot be null");

        validateSurveyRunUpdate(userName, surveyRunId);

        return surveyRunDao.update(surveyRunId, command);
    }


    public int updateSurveyRunStatus(String userName, long surveyRunId, SurveyRunStatus newStatus) {
        checkNotNull(userName, "userName cannot be null");
        checkNotNull(newStatus, "newStatus cannot be null");

        validateSurveyRunUpdate(userName, surveyRunId);

        int result = (newStatus == SurveyRunStatus.ISSUED)
                ? surveyRunDao.issue(surveyRunId)
                : surveyRunDao.updateStatus(surveyRunId, newStatus);

        changeLogService.write(
                ImmutableChangeLog.builder()
                        .operation(Operation.UPDATE)
                        .userId(userName)
                        .parentReference(EntityReference.mkRef(EntityKind.SURVEY_RUN, surveyRunId))
                        .message("Survey Run: status changed to " + newStatus)
                        .build());

        return result;
    }


    public int updateSurveyRunDueDate(String userName, long surveyRunId, DateChangeCommand command) {
        checkNotNull(userName, "userName cannot be null");
        checkNotNull(command, "command cannot be null");

        LocalDate newDueDate = command.newDateVal().orElse(null);

        checkNotNull(newDueDate, "newDueDate cannot be null");

        int surveyRunResult = surveyRunDao.updateDueDate(surveyRunId, newDueDate);
        int surveyInstanceResult = surveyInstanceDao.updateDueDateForSurveyRun(surveyRunId, newDueDate);

        changeLogService.write(
                ImmutableChangeLog.builder()
                        .operation(Operation.UPDATE)
                        .userId(userName)
                        .parentReference(EntityReference.mkRef(EntityKind.SURVEY_RUN, surveyRunId))
                        .message("Survey Run: due date changed to " + newDueDate)
                        .build());

        return surveyRunResult + surveyInstanceResult;
    }


    public List<SurveyInstanceRecipient> generateSurveyInstanceRecipients(long surveyRunId) {
        SurveyRun surveyRun = surveyRunDao.getById(surveyRunId);
        checkNotNull(surveyRun, "surveyRun " + surveyRunId + " not found");

        SurveyTemplate surveyTemplate = surveyTemplateDao.getById(surveyRun.surveyTemplateId());
        checkNotNull(surveyTemplate, "surveyTemplate " + surveyRun.surveyTemplateId() + " not found");

        GenericSelector genericSelector = genericSelectorFactory.applyForKind(surveyTemplate.targetEntityKind(), surveyRun.selectionOptions());
        Map<EntityReference, List<Person>> entityRefToPeople = involvementDao.findPeopleByEntitySelectorAndInvolvement(
                surveyTemplate.targetEntityKind(),
                genericSelector.selector(),
                surveyRun.involvementKindIds());

        return entityRefToPeople.entrySet()
                .stream()
                .flatMap(e -> e.getValue().stream()
                        .map(p -> ImmutableSurveyInstanceRecipient.builder()
                                .surveyInstance(ImmutableSurveyInstance.builder()
                                        .surveyEntity(e.getKey())
                                        .surveyRunId(surveyRun.id().get())
                                        .status(SurveyInstanceStatus.NOT_STARTED)
                                        .dueDate(surveyRun.dueDate())
                                        .build())
                                .person(p)
                                .build()))
                .distinct()
                .collect(toList());
    }


    public boolean createSurveyInstancesAndRecipients(long surveyRunId,
                                                   List<SurveyInstanceRecipient> excludedRecipients) {
        SurveyRun surveyRun = surveyRunDao.getById(surveyRunId);
        checkNotNull(surveyRun, "surveyRun " + surveyRunId + " not found");

        Set<SurveyInstanceRecipient> excludedRecipientSet = fromCollection(excludedRecipients);
        List<SurveyInstanceRecipient> surveyInstanceRecipients = generateSurveyInstanceRecipients(surveyRunId).stream()
                .filter(r -> !excludedRecipientSet.contains(r))
                .collect(toList());

        Map<SurveyInstance, List<SurveyInstanceRecipient>> instancesAndRecipientsToSave = surveyInstanceRecipients.stream()
                .collect(groupingBy(
                        SurveyInstanceRecipient::surveyInstance,
                        toList()
                ));


        // delete existing instances and recipients
        deleteSurveyInstancesAndRecipients(surveyRunId);

        // insert new instances and recipients
        instancesAndRecipientsToSave.forEach(
                (k,v) -> {
                    if (surveyRun.issuanceKind() == SurveyIssuanceKind.GROUP) {
                        // one instance per group
                        long instanceId = createSurveyInstance(k);
                        v.forEach(r -> createSurveyInstanceRecipient(instanceId, r));
                    } else {
                        // one instance for each individual
                        v.forEach(r -> {
                            long instanceId = createSurveyInstance(k);
                            createSurveyInstanceRecipient(instanceId, r);
                        });
                    }
                }
        );

        return true;
    }


    private void deleteSurveyInstancesAndRecipients(long surveyRunId) {
        surveyInstanceRecipientDao.deleteForSurveyRun(surveyRunId);
        surveyInstanceDao.deleteForSurveyRun(surveyRunId);
    }


    private long createSurveyInstance(SurveyInstance surveyInstance) {
        return surveyInstanceDao.create(ImmutableSurveyInstanceCreateCommand.builder()
                .surveyRunId(surveyInstance.surveyRunId())
                .entityReference(surveyInstance.surveyEntity())
                .status(surveyInstance.status())
                .dueDate(surveyInstance.dueDate())
                .build());
    }


    private long createSurveyInstanceRecipient(long surveyInstanceId, SurveyInstanceRecipient surveyInstanceRecipient) {
        return surveyInstanceRecipientDao.create(ImmutableSurveyInstanceRecipientCreateCommand.builder()
                .surveyInstanceId(surveyInstanceId)
                .personId(surveyInstanceRecipient.person().id().get())
                .build());
    }


    private void validateSurveyRunUpdate(String userName, long surveyRunId) {
        Person owner = personDao.getActiveByUserEmail(userName);
        checkNotNull(owner, "userName " + userName + " cannot be resolved");

        SurveyRun surveyRun = surveyRunDao.getById(surveyRunId);
        checkNotNull(surveyRun, "surveyRun " + surveyRunId + " not found");

        checkTrue(Objects.equals(surveyRun.ownerId(), owner.id().get()), "Permission denied");

        checkTrue(surveyRun.status() == SurveyRunStatus.DRAFT, "survey run can only be updated when it's still in DRAFT mode");
    }


    public List<SurveyRun> findBySurveyInstanceIdSelector(IdSelectionOptions idSelectionOptions) {
        checkNotNull(idSelectionOptions,  "idSelectionOptions cannot be null");

        Select<Record1<Long>> selector = surveyInstanceIdSelectorFactory.apply(idSelectionOptions);

        return surveyRunDao.findBySurveyInstanceIdSelector(selector);
    }


    public List<SurveyRunWithOwnerAndStats> findByTemplateId(long templateId) {
        List<SurveyRun> runs = surveyRunDao.findByTemplateId(templateId);
        Set<Person> owners = personDao.findByIds(SetUtilities.map(runs, SurveyRun::ownerId));
        Map<Long, Person> ownersById = indexBy(owners, p -> p.id().orElse(-1L));

        List<SurveyRunCompletionRate> stats = surveyInstanceDao.findCompletionRateForSurveyTemplate(templateId);
        Map<Long, SurveyRunCompletionRate> statsByRunId = indexBy(stats, s -> s.surveyRunId());

        return map(runs, run -> {
            Person owner = ownersById.get(run.ownerId());
            Long runId = run.id().orElse(-1L);
            SurveyRunCompletionRate completionRateStats = statsByRunId.getOrDefault(
                    runId,
                    SurveyRunCompletionRate.mkNoData(runId));

            return ImmutableSurveyRunWithOwnerAndStats.builder()
                    .surveyRun(run)
                    .owner(owner)
                    .completionRateStats(completionRateStats)
                    .build();
        });
    }


    public SurveyRunCompletionRate getCompletionRate(long surveyRunId) {
        return surveyInstanceDao.getCompletionRateForSurveyRun(surveyRunId);
    }


    public boolean createDirectSurveyInstances(long runId, List<Long> personIds) {
        SurveyRun run = getById(runId);
        EntityReference subjectRef = run.selectionOptions().entityReference();

        switch (run.issuanceKind()) {
            case INDIVIDUAL:
                map(personIds, p -> mkSurveyInstance(
                        subjectRef,
                        run,
                        ListUtilities.newArrayList(p)));
                return true;
            case GROUP:
                mkSurveyInstance(
                        subjectRef,
                        run,
                        personIds);
                return true;
            default:
                return false;
        }
    }

    private int[] mkSurveyInstance(EntityReference entityRef,
                                   SurveyRun run,
                                   List<Long> personIds) {
        SurveyInstanceCreateCommand instanceCreateCommand = ImmutableSurveyInstanceCreateCommand
                .builder()
                .dueDate(run.dueDate())
                .entityReference(entityRef)
                .surveyRunId(run.id().get())
                .status(SurveyInstanceStatus.NOT_STARTED)
                .build();
        long instanceId = surveyInstanceDao.create(instanceCreateCommand);
        return surveyInstanceDao.createInstanceRecipients(
                instanceId,
                personIds);
    }
}

