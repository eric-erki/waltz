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
import {buildHierarchies, doSearch, prepareSearchNodes} from "../../../common/hierarchy-utils";
import template from "./measurable-rating-tree.html";
import {truncateMiddle} from "../../../common/string-utils";

/**
 * @name waltz-measurable-rating-tree
 *
 * @description
 * Tree control used to show measurables and their ratings.
 *
 * Intended only for use with a single application and a single measurable kind.
 */
const bindings = {
    allocations: "<?",
    ratings: "<",
    ratingScheme: "<",
    measurables: "<",
    onKeypress: "<",
    onSelect: "<",
    scrollHeight: "@" // should correspond to numeric values in `waltz-scroll-region` classes
};


const initialState = {
    allocations: [],
    containerClass: "",
    hierarchy: [],
    measurables: [],
    ratings: [],
    ratingScheme: null,
    searchTerms: "",
    treeOptions: {
        nodeChildren: "children",
        dirSelectable: true,
        equality: function(node1, node2) {
            if (node1 && node2) {
                return node1.id === node2.id;
            } else {
                return false;
            }
        }
    },
    onKeypress: null,
    onSelect: (m, r) => console.log("default on-select for measurable-rating-tree: ", m, r)
};


// expand nodes with a rating (incl. parents)
function calculateExpandedNodes(nodes = [], ratingsById = {}) {
    const byId = _.keyBy(nodes, "id");
    const startingNodes = _.filter(nodes, n => ratingsById[n.id] != null);

    const requiredIds = [];
    _.each(startingNodes, n => {
        requiredIds.push(n.id);
        while (n && n.parentId) {
            requiredIds.push(n.parentId);
            const parent = byId[n.parentId];
            if (! parent) console.warn(`WMRE: could not find parent (${n.parentId}) of measurable (${n.id})`);
            n = parent;
        }
    });

    // de-dupe and resolve
    return _.map(
        _.uniq(requiredIds),
        id => byId[id]);
}


function prepareTree(nodes = []) {
    return buildHierarchies(nodes, false);
}

function controller() {
    const vm = this;

    vm.$onInit = () => initialiseData(vm, initialState);

    vm.$onChanges = (c) => {
        const toDisplayName = m => truncateMiddle(m.name, 96);
        const measurables = _.map(vm.measurables, m => Object.assign({}, m, { displayName: toDisplayName(m)}));
        vm.searchNodes = prepareSearchNodes(measurables);
        vm.hierarchy = prepareTree(measurables);
        vm.ratingsByMeasurable = _.keyBy(vm.ratings || [], "measurableId");
        vm.ratingsByCode = _.keyBy(_.get(vm.ratingScheme, "ratings", []), "rating");
        vm.allocationsByMeasurable = _.groupBy(vm.allocations, d => d.measurableId);

        if (_.isEmpty(vm.expandedNodes)) {
            vm.expandedNodes = calculateExpandedNodes(measurables, vm.ratingsByMeasurable);
        }

        if (c.scrollHeight) {
            vm.containerClass = `waltz-scroll-region-${vm.scrollHeight}`;
        }
    };

    vm.searchTermsChanged = (termStr = "") => {
        vm.hierarchy = prepareTree(doSearch(termStr, vm.searchNodes));
    };

    vm.clearSearch = () => {
        vm.searchTermsChanged("");
        vm.searchTerms = "";
    };
}


controller.$inject = [];


const component = {
    template,
    bindings,
    controller
};


export default {
    component,
    id: "waltzMeasurableRatingTree"
};
