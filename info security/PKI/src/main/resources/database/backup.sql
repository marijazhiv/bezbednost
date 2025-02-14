;              
CREATE USER IF NOT EXISTS "SA" SALT 'afa840e5e24a77b4' HASH 'c68406f00657b916dee3bb740aeb0256984e7921e7dd5147b5bdb9c5f863a94c' ADMIN;          
CREATE SEQUENCE "PUBLIC"."SYSTEM_SEQUENCE_5753EB46_B729_42E9_974B_E507D5AF0EFD" START WITH 3 BELONGS_TO_TABLE; 
CREATE CACHED TABLE "PUBLIC"."CERTIFICATE_REQUEST"(
    "ID" BIGINT DEFAULT NEXT VALUE FOR "PUBLIC"."SYSTEM_SEQUENCE_5753EB46_B729_42E9_974B_E507D5AF0EFD" NOT NULL NULL_TO_DEFAULT SEQUENCE "PUBLIC"."SYSTEM_SEQUENCE_5753EB46_B729_42E9_974B_E507D5AF0EFD",
    "CERTIFICATE_REQUEST_STATUS" VARCHAR(255) CHECK ("CERTIFICATE_REQUEST_STATUS" IN('PENDING', 'ACCEPTED', 'REJECTED')),
    "CERTIFICATE_TYPE" VARCHAR(255) CHECK ("CERTIFICATE_TYPE" IN('ROOT', 'INTERMEDIATE', 'END_ENTITY')),
    "COUNTRY" VARCHAR(255),
    "EMAIL" VARCHAR(255),
    "LOCALITY" VARCHAR(255),
    "SUBJECT_NAME" VARCHAR(255)
);         
ALTER TABLE "PUBLIC"."CERTIFICATE_REQUEST" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_F" PRIMARY KEY("ID");           
-- 2 +/- SELECT COUNT(*) FROM PUBLIC.CERTIFICATE_REQUEST;      
INSERT INTO "PUBLIC"."CERTIFICATE_REQUEST" VALUES
(1, 'PENDING', 'END_ENTITY', '56', '56@5z5z.com', '565', 'u6'),
(2, 'PENDING', 'END_ENTITY', '5sd6', '56q@5z5z.com', 'sd', 'sd');            
