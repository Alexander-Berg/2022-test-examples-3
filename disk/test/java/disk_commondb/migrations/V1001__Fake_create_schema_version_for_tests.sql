--for work of tests (grants/monitor.sql:4 fails without it, GRANT SELECT ON public.schema_version TO monitor)
CREATE TABLE IF NOT EXISTS public.schema_version(version numeric);
