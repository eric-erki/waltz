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

import {checkIsApplicationIdSelector, checkIsIdSelector} from "../../common/checks";


export function store($http,
                      baseUrl) {

    const BASE = `${baseUrl}/logical-flow-decorator`;

    const findBySelectorAndKind = (selector, kind = 'DATA_TYPE') => {
        checkIsApplicationIdSelector(selector);
        return $http
            .post(`${BASE}/selector/kind/${kind}`, selector)
            .then(result => result.data);
    };


    const findByFlowIdsAndKind = (flowIds = [], kind = 'DATA_TYPE') => {
        return $http
            .post(`${BASE}/flow-ids/kind/${kind}`, flowIds)
            .then(result => result.data);
    };


    const findBySelector = (selector) => {
        checkIsIdSelector(selector);
        return $http
            .post(`${BASE}/selector`, selector)
            .then(result => result.data);
    };


    const findDataTypeStatsForEntity = (selector) => {
        checkIsIdSelector(selector);
        return $http
            .post(`${BASE}/datatype-stats`, selector)
            .then(result => result.data);
    };


    /**
     * where command like:
     *    {
     *      flowId: <number>,
     *      addedDecorators: [<entityRef>],
     *      removedDecorators: [<entityRef>]
     *    }
     *
     * @param command
     * @returns {*|Promise.<FlowDecorator>}
     */
    const updateDecorators = (command) => {
        return $http
            .post(`${BASE}/update/flow/${command.flowId}`, command)
            .then(r => r.data);
    };


    /**
     * where command like:
     *    [{
     *      flowId: <number>,
     *      addedDecorators: [<entityRef>],
     *      removedDecorators: [<entityRef>]
     *    }]
     *
     * @param command
     * @returns {*|Promise.<[FlowDecorator]>}
     */
    const updateDecoratorsBatch = (commands) => {
        return $http
            .post(`${BASE}/update/batch`, commands);
    };


        /**
     * Given a selector returns a promising giving results like:
     *
     *      [
     *          { decoratorEntityReference : <entityRef>,
     *            rating: PRIMARY | SECONDARY | NO_OPINION | DISCOURAGED,
     *            count: <number>
     *          }
     *      ]
     *
     * @param selector
     * @returns {*|Promise.<TResult>}
     */
    const summarizeInboundBySelector = (selector) => {
        checkIsApplicationIdSelector(selector);
        return $http.post(`${BASE}/summarize-inbound`, selector)
            .then(r => r.data);
    };

    const summarizeOutboundBySelector = (selector) => {
        checkIsApplicationIdSelector(selector);
        return $http.post(`${BASE}/summarize-outbound`, selector)
            .then(r => r.data);
    };

    const summarizeInboundForAll = () => {
        return $http.get(`${BASE}/summarize-inbound`)
            .then(r => r.data);
    };

    return {
        findBySelectorAndKind,
        findBySelector,
        findByFlowIdsAndKind,
        findDataTypeStatsForEntity,
        updateDecorators,
        updateDecoratorsBatch,
        summarizeInboundBySelector,
        summarizeOutboundBySelector,
        summarizeInboundForAll
    };
}

store.$inject = [
    '$http',
    'BaseApiUrl'
];


export const serviceName = "LogicalFlowDecoratorStore";


export const LogicalFlowDecoratorStore_API = {
    findBySelectorAndKind: {
        serviceName,
        serviceFnName: 'findBySelectorAndKind',
        description: 'executes findBySelectorAndKind'
    },
    findBySelector: {
        serviceName,
        serviceFnName: 'findBySelector',
        description: 'executes findBySelector'
    },
    findByFlowIdsAndKind: {
        serviceName,
        serviceFnName: 'findByFlowIdsAndKind',
        description: 'executes findByFlowIdsAndKind - arg1: [flowIds], arg2: decoratorKind'
    },
    findDataTypeStatsForEntity: {
        serviceName,
        serviceFnName: 'findDataTypeStatsForEntity',
        description: 'executes findDataTypeStatsForEntity'
    },
    updateDecorators: {
        serviceName,
        serviceFnName: 'updateDecorators',
        description: 'executes updateDecorators'
    },
    updateDecoratorsBatch: {
        serviceName,
        serviceFnName: 'updateDecoratorsBatch',
        description: 'executes updateDecoratorsBatch'
    },
    summarizeInboundBySelector: {
        serviceName,
        serviceFnName: 'summarizeInboundBySelector',
        description: 'executes summarizeInboundBySelector - arg1: app_selector'
    },
    summarizeOutboundBySelector: {
        serviceName,
        serviceFnName: 'summarizeOutboundBySelector',
        description: 'executes summarizeOutboundBySelector - arg1: app_selector'
    },
    summarizeInboundForAll: {
        serviceName,
        serviceFnName: 'summarizeInboundForAll',
        description: 'executes summarizeForAll (no args)'
    }
};
