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
import _ from "lodash";
import {initialiseData} from "../../../common";
import template from "./measurable-rating-panel.html";

/**
 * @name waltz-measurable-rating-panel
 *
 * @description
 * This component wraps a <code>measurable-rating-tree</code>
 * and provides a detail view when a node is clicked on.
 *
 * It is intended to be used when viewing a single application
 * and a single 'kind' of measurables.
 */


const bindings = {
    allocations: "<",
    allocationSchemes: "<",
    category: "<",
    entityReference: "<",
    measurables: "<",
    ratings: "<",
    ratingScheme: "<"
};


const initialState = {
    allocations: [],
    allocationSchemes: [],
    measurables: [],
    ratings: [],
    selected: null
};


function controller() {
    const vm = this;

    vm.$onInit = () => initialiseData(vm, initialState);

    vm.$onChanges = () => {
        vm.ratingsByCode = _.keyBy(_.get(vm.ratingScheme, "ratings", []), r => r.rating);
        vm.allocationSchemesById = _.keyBy(vm.allocationSchemes, s => s.id);
    };

    vm.onSelect = (measurable, rating) => {
        const relevantAllocations = _
            .chain(vm.allocations)
            .filter(a => a.measurableId === measurable.id)
            .map(allocation => {
                const scheme = vm.allocationSchemesById[allocation.schemeId];
                return scheme
                    ? Object.assign(allocation, {scheme})
                    : null;
            })
            .compact()
            .sortBy(a => a.scheme.name)
            .value();

        vm.selected = {
            allocations: relevantAllocations,
            measurable,
            rating,
        };
    }
}


controller.$inject = [];


const component = {
    template,
    bindings,
    controller
};



export default {
    component,
    id: "waltzMeasurableRatingPanel"
};
