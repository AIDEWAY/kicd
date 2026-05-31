CREATE TABLE icd_meta(
    version VARCHAR(20),
    title VARCHAR(100),
    created_at INTEGER
);

CREATE TABLE icd_nodes(
    id INTEGER UNIQUE,
    name VARCHAR(10) NOT NULL,
    description VARCHAR(100),
    parent_id INTEGER,
    type_id INTEGER NOT NULL,
    billable INTEGER,
    notes TEXT,
    includes TEXT,
    inclusion_terms TEXT,
    excludes1 TEXT,
    excludes2 TEXT,
    code_first TEXT,
    code_also TEXT,
    use_additional_code TEXT,
    FOREIGN KEY (parent_id) REFERENCES icd_nodes(id)
);

CREATE TABLE icd_node_clinical_details(
    icd_node_id INTEGER UNIQUE,
    clinical_details TEXT NOT NULL
);
