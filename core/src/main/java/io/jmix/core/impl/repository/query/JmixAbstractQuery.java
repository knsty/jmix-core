/*
 * Copyright 2021 Haulmont.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.core.impl.repository.query;

import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.impl.repository.query.utils.JmixQueryLookupStrategy;
import io.jmix.core.impl.repository.support.method_metadata.MethodMetadataHelper;
import io.jmix.core.repository.ApplyConstraints;
import io.jmix.core.repository.FetchPlan;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Query implementation for Jmix. If you need different types of queries, you can either extend this class or implement parent interface.
 *
 * @see JmixQueryLookupStrategy is responsible for generating Query implementations based on interface method names that will be executed by the Jmix.
 * @see RepositoryQuery
 */
public abstract class JmixAbstractQuery implements RepositoryQuery {

    protected final Method method;
    protected final RepositoryMetadata metadata;
    protected final ProjectionFactory factory;
    protected final QueryMethod queryMethod;
    /**
     * {@link UnconstrainedDataManager} or {@link DataManager} will be chosen depending on {@link ApplyConstraints} annotation on method/repository or ancestor method/repository
     */
    protected UnconstrainedDataManager dataManager;

    protected Metadata jmixMetadata;

    protected Map<String, Integer> namedParametersBindings = new HashMap<>();


    protected int sortIndex;
    protected int pageableIndex;

    protected String fetchPlan = io.jmix.core.FetchPlan.LOCAL;

    public JmixAbstractQuery(DataManager dataManager, Metadata jmixMetadata, Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        this.method = method;
        this.metadata = metadata;
        this.factory = factory;
        this.queryMethod = getQueryMethod();
        this.jmixMetadata = jmixMetadata;

        ApplyConstraints applyConstraintsAnnotation = MethodMetadataHelper.determineApplyConstraints(method, metadata.getRepositoryInterface());
        this.dataManager = applyConstraintsAnnotation.value() ? dataManager : dataManager.unconstrained();

        setFetchPlan(method);
        processSpecialParameters();
    }

    @Override
    public QueryMethod getQueryMethod() {
        return new QueryMethod(method, metadata, factory);
    }

    public UnconstrainedDataManager getDataManager() {
        return dataManager;
    }

    private void setFetchPlan(Method method) {
        FetchPlan fetchPlanAnnotation = method.getDeclaredAnnotation(FetchPlan.class);
        if (fetchPlanAnnotation != null) {
            fetchPlan = fetchPlanAnnotation.value();
        }

    }

    protected Map<String, Object> buildNamedParametersMap(Object[] values) {
        Map<String, Object> paramsMap = new HashMap<>();
        for (Map.Entry<String, Integer> parameterBinding : namedParametersBindings.entrySet()) {
            paramsMap.put(parameterBinding.getKey(), values[parameterBinding.getValue()]);
        }
        return paramsMap;
    }

    protected void processSpecialParameters() {
        Parameters parameters = queryMethod.getParameters();

        pageableIndex = parameters.getPageableIndex();
        sortIndex = parameters.getSortIndex();
    }

    protected static String formatMethod(Method method) {
        return method.getDeclaringClass().getName() + '#' + method.getName();

    }

    @Override
    public String toString() {
        return String.format("%s:{%s}", this.getClass().getSimpleName(), getQueryDescription());
    }

    protected String getQueryDescription() {
        return String.format("fetchPlan:'%s'; sortIndex:'%s'; pageableIndex:'%s'", fetchPlan, sortIndex, pageableIndex);
    }
}
