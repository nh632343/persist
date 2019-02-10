package com.jinke.persist.config;

import com.jinke.persist.enums.ConflictAction;

public class OPOption<T> {
    private TableNameOverride tableNameOverride;

    private ConflictOption conflictOption;

    public OPOption<T> withTableNameOverride(TableNameOverride tableNameOverride) {
        this.tableNameOverride = tableNameOverride;
        return this;
    }

    public TableNameOverride getTableNameOverride() {
        return tableNameOverride;
    }

    public OPOption<T> withConflictOption(ConflictOption conflictOption) {
        this.conflictOption = conflictOption;
        return this;
    }

    public ConflictOption getConflictOption() {
        return conflictOption;
    }

    public static class ConflictOption {
        private String constraint;

        private ConflictAction conflictAction;

        private String updateFieldGroup;

        public ConflictOption withConstraint(String constraint) {
            this.constraint = constraint;
            return this;
        }

        public ConflictOption withConflictAction(ConflictAction conflictAction) {
            this.conflictAction = conflictAction;
            return this;
        }

        public ConflictOption withUpdateField(String updateField) {
            this.updateFieldGroup = updateField;
            return this;
        }

        public String getConstraint() {
            return constraint;
        }

        public ConflictAction getConflictAction() {
            return conflictAction;
        }

        public String getUpdateFieldGroup() {
            return updateFieldGroup;
        }

        @Override
        public String toString() {
            return "ConflictOption{" +
                    "constraint='" + constraint + '\'' +
                    ", conflictAction=" + conflictAction +
                    ", updateFieldGroup='" + updateFieldGroup + '\'' +
                    '}';
        }
    }
}
