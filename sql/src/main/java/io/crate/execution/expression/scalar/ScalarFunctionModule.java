/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.execution.expression.scalar;

import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionImplementation;
import io.crate.metadata.FunctionResolver;
import io.crate.execution.expression.scalar.arithmetic.AbsFunction;
import io.crate.execution.expression.scalar.arithmetic.ArithmeticFunctions;
import io.crate.execution.expression.scalar.arithmetic.ArrayFunction;
import io.crate.execution.expression.scalar.arithmetic.CeilFunction;
import io.crate.execution.expression.scalar.arithmetic.FloorFunction;
import io.crate.execution.expression.scalar.arithmetic.LogFunction;
import io.crate.execution.expression.scalar.arithmetic.MapFunction;
import io.crate.execution.expression.scalar.arithmetic.NegateFunction;
import io.crate.execution.expression.scalar.arithmetic.RandomFunction;
import io.crate.execution.expression.scalar.arithmetic.RoundFunction;
import io.crate.execution.expression.scalar.arithmetic.SquareRootFunction;
import io.crate.execution.expression.scalar.arithmetic.TrigonometricFunctions;
import io.crate.execution.expression.scalar.cast.CastFunction;
import io.crate.execution.expression.scalar.cast.TryCastScalarFunction;
import io.crate.execution.expression.scalar.conditional.CoalesceFunction;
import io.crate.execution.expression.scalar.conditional.GreatestFunction;
import io.crate.execution.expression.scalar.conditional.IfFunction;
import io.crate.execution.expression.scalar.conditional.LeastFunction;
import io.crate.execution.expression.scalar.conditional.NullIfFunction;
import io.crate.execution.expression.scalar.geo.CoordinateFunction;
import io.crate.execution.expression.scalar.geo.DistanceFunction;
import io.crate.execution.expression.scalar.geo.GeoHashFunction;
import io.crate.execution.expression.scalar.geo.IntersectsFunction;
import io.crate.execution.expression.scalar.geo.WithinFunction;
import io.crate.execution.expression.scalar.postgres.PgBackendPidFunction;
import io.crate.execution.expression.scalar.regex.MatchesFunction;
import io.crate.execution.expression.scalar.regex.ReplaceFunction;
import io.crate.execution.expression.scalar.string.HashFunctions;
import io.crate.execution.expression.scalar.string.LengthFunction;
import io.crate.execution.expression.scalar.string.StringCaseFunction;
import io.crate.execution.expression.scalar.systeminformation.CurrentSchemaFunction;
import io.crate.execution.expression.scalar.timestamp.CurrentTimestampFunction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import java.util.HashMap;
import java.util.Map;

public class ScalarFunctionModule extends AbstractModule {

    private Map<FunctionIdent, FunctionImplementation> functions = new HashMap<>();
    private Map<String, FunctionResolver> resolver = new HashMap<>();
    private MapBinder<FunctionIdent, FunctionImplementation> functionBinder;
    private MapBinder<String, FunctionResolver> resolverBinder;

    public void register(FunctionImplementation impl) {
        functions.put(impl.info().ident(), impl);
    }

    public void register(String name, FunctionResolver functionResolver) {
        resolver.put(name, functionResolver);
    }

    @Override
    protected void configure() {
        NegateFunction.register(this);
        CollectionCountFunction.register(this);
        CollectionAverageFunction.register(this);
        FormatFunction.register(this);
        SubstrFunction.register(this);
        MatchesFunction.register(this);
        ReplaceFunction.register(this);

        ArithmeticFunctions.register(this);

        DistanceFunction.register(this);
        WithinFunction.register(this);
        IntersectsFunction.register(this);
        CoordinateFunction.register(this);
        GeoHashFunction.register(this);

        SubscriptFunction.register(this);
        SubscriptObjectFunction.register(this);

        RoundFunction.register(this);
        CeilFunction.register(this);
        RandomFunction.register(this);
        AbsFunction.register(this);
        FloorFunction.register(this);
        SquareRootFunction.register(this);
        LogFunction.register(this);
        TrigonometricFunctions.register(this);

        DateTruncFunction.register(this);
        ExtractFunctions.register(this);
        CurrentTimestampFunction.register(this);
        DateFormatFunction.register(this);
        CastFunction.register(this);
        TryCastScalarFunction.register(this);

        StringCaseFunction.register(this);

        ConcatFunction.register(this);

        LengthFunction.register(this);
        HashFunctions.register(this);

        MapFunction.register(this);
        ArrayFunction.register(this);
        ArrayCatFunction.register(this);
        ArrayDifferenceFunction.register(this);
        ArrayUniqueFunction.register(this);

        CoalesceFunction.register(this);
        GreatestFunction.register(this);
        LeastFunction.register(this);
        NullIfFunction.register(this);
        IfFunction.register(this);

        CurrentSchemaFunction.register(this);

        PgBackendPidFunction.register(this);

        // bind all registered functions and resolver
        // by doing it here instead of the register functions, plugins can also use the
        // register functions in their onModule(...) hooks
        functionBinder = MapBinder.newMapBinder(binder(), FunctionIdent.class, FunctionImplementation.class);
        resolverBinder = MapBinder.newMapBinder(binder(), String.class, FunctionResolver.class);
        for (Map.Entry<FunctionIdent, FunctionImplementation> entry : functions.entrySet()) {
            functionBinder.addBinding(entry.getKey()).toInstance(entry.getValue());

        }
        for (Map.Entry<String, FunctionResolver> entry : resolver.entrySet()) {
            resolverBinder.addBinding(entry.getKey()).toInstance(entry.getValue());
        }

        // clear registration maps
        functions = null;
        resolver = null;
    }
}