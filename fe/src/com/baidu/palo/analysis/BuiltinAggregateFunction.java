// Modifications copyright (C) 2017, Baidu.com, Inc.
// Copyright 2017 The Apache Software Foundation

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.baidu.palo.analysis;

import com.baidu.palo.catalog.Function;
import com.baidu.palo.catalog.ScalarType;
import com.baidu.palo.catalog.Type;
import com.baidu.palo.common.AnalysisException;
import com.baidu.palo.thrift.TAggregateFunction;
import com.baidu.palo.thrift.TAggregationOp;
import com.baidu.palo.thrift.TFunction;
import com.baidu.palo.thrift.TFunctionBinaryType;
import com.google.common.base.Preconditions;

import java.util.ArrayList;

/**
 * Internal representation of a builtin aggregate function.
 */
public class BuiltinAggregateFunction extends Function {
    private final Operator                          op_;
    // this is to judge the analytic function
    private boolean isAnalyticFn = false;
    
    public boolean isAnalyticFn() {
        return isAnalyticFn;
    }
    // TODO: this is not used yet until the planner understand this.
    private       com.baidu.palo.catalog.Type intermediateType_;
    private boolean reqIntermediateTuple = false;

    public boolean isReqIntermediateTuple() {
        return reqIntermediateTuple;
    }
    
    public BuiltinAggregateFunction(Operator op, ArrayList<Type> argTypes,
      Type retType, com.baidu.palo.catalog.Type intermediateType, boolean isAnalyticFn)
      throws AnalysisException {
        super(FunctionName.CreateBuiltinName(op.toString()), argTypes,
          retType, false);
        Preconditions.checkState(intermediateType != null);
        Preconditions.checkState(op != null);
        // may be no need to analyze
        // intermediateType.analyze();
        op_ = op;
        intermediateType_ = intermediateType;
        if (isAnalyticFn && !intermediateType.equals(retType)) {
            reqIntermediateTuple = true;
        }
        setBinaryType(TFunctionBinaryType.BUILTIN);
        this.isAnalyticFn = isAnalyticFn;
    }

    @Override
    public TFunction toThrift() {
        TFunction fn = super.toThrift();
        // TODO: for now, just put the op_ enum as the id.
        if (op_ == BuiltinAggregateFunction.Operator.FIRST_VALUE_REWRITE) {
            fn.setId(0);
        } else {
            fn.setId(op_.thriftOp.ordinal());
        }
        fn.setAggregate_fn(new TAggregateFunction(intermediateType_.toThrift()));
        return fn;
    }

    public Operator op() {
        return op_;
    }

    public com.baidu.palo.catalog.Type getIntermediateType() {
        return intermediateType_;
    }

    public void setIntermediateType(com.baidu.palo.catalog.Type t) {
        intermediateType_ = t;
    }

    // TODO: this is effectively a catalog of builtin aggregate functions.
    // We should move this to something in the catalog instead of having it
    // here like this.
    public enum Operator {
        COUNT("COUNT", TAggregationOp.COUNT, Type.BIGINT),
        MIN("MIN", TAggregationOp.MIN, null),
        MAX("MAX", TAggregationOp.MAX, null),
        DISTINCT_PC("DISTINCT_PC", TAggregationOp.DISTINCT_PC, ScalarType.createVarcharType(64)),
        DISTINCT_PCSA("DISTINCT_PCSA", TAggregationOp.DISTINCT_PCSA, ScalarType.createVarcharType(64)),
        SUM("SUM", TAggregationOp.SUM, null),
        AVG("AVG", TAggregationOp.INVALID, null),
        GROUP_CONCAT("GROUP_CONCAT", TAggregationOp.GROUP_CONCAT, ScalarType.createVarcharType(16)),

        // NDV is the external facing name (i.e. queries should always be written with NDV)
        // The current implementation of NDV is hyperloglog (but we could change this without
        // external query changes if we find a better algorithm).
        NDV("NDV", TAggregationOp.HLL, ScalarType.createVarcharType(64)),
        HLL_UNION_AGG("HLL_UNION_AGG", TAggregationOp.HLL_C, ScalarType.createVarcharType(64)),
        COUNT_DISTINCT("COUNT_DISITNCT", TAggregationOp.COUNT_DISTINCT, Type.BIGINT),
        SUM_DISTINCT("SUM_DISTINCT", TAggregationOp.SUM_DISTINCT, null),
        LAG("LAG", TAggregationOp.LAG, null),
        FIRST_VALUE("FIRST_VALUE", TAggregationOp.FIRST_VALUE, null),
        LAST_VALUE("LAST_VALUE", TAggregationOp.LAST_VALUE, null),
        RANK("RANK", TAggregationOp.RANK, null),
        DENSE_RANK("DENSE_RANK", TAggregationOp.DENSE_RANK, null),
        ROW_NUMBER("ROW_NUMBER", TAggregationOp.ROW_NUMBER, null),
        LEAD("LEAD", TAggregationOp.LEAD, null),
        FIRST_VALUE_REWRITE("FIRST_VALUE_REWRITE", null, null);

        private final String         description;
        private final TAggregationOp thriftOp;

        // The intermediate type for this function if it is constant regardless of
        // input type. Set to null if it can only be determined during analysis.
        private final com.baidu.palo.catalog.Type intermediateType;
        private Operator(String description, TAggregationOp thriftOp,
          com.baidu.palo.catalog.Type intermediateType) {
            this.description = description;
            this.thriftOp = thriftOp;
            this.intermediateType = intermediateType;
        }

        @Override
        public String toString() {
            return description;
        }

        public TAggregationOp toThrift() {
            return thriftOp;
        }

        public com.baidu.palo.catalog.Type intermediateType() {
            return intermediateType;
        }
    }
}
