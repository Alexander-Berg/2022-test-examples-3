if (object_id('dbo.sp_moderation_creative_get_test_assembly_list', 'P') is not null)
	drop procedure dbo.sp_moderation_creative_get_test_assembly_list;
go

set ansi_nulls on;
go

-- Вернуть список тестовых сборок креатива для ui модерации
create procedure dbo.sp_moderation_creative_get_test_assembly_list
	@creative_nmb int,
	@creative_version_nmb int = null
as
begin
	-- Берем последнюю версию, если ничего не задали
	if (@creative_version_nmb is null)
		set @creative_version_nmb = (select max(nmb) from dbo.t_creative_version where creative_nmb = @creative_nmb);
	
	if (@creative_version_nmb is null)
		throw 50000, '@creative_nmb or @creative_version_nmb must not be NULL', 1;

	declare @template_version_nmb int;
	declare @width int;
	declare @height int;
	select
			@creative_nmb = creative_nmb, -- @creative_nmb может быть не задан, так что здесь немного оптимизируем
			@template_version_nmb = template_version_nmb,
			@width = coalesce(width, dbo.uf_creative_version_get_width(@creative_version_nmb)),
			@height = coalesce(height, dbo.uf_creative_version_get_height(@creative_version_nmb))
	from dbo.t_creative_version
	where nmb = @creative_version_nmb;

  -- [кастыль] Если креатив это смарт-тго то возвращаем прибитые гвоздями ширину и высоту (для него в превью показывается гифка)
  if exists(select top 1 *
              from t_template_version_layout_code tvlc
                join t_creative_version cv on cv.layout_code_nmb = tvlc.nmb and cv.nmb = @creative_version_nmb
              where tvlc.layout_nmb = 44) BEGIN
			SELECT @width = 310,
			       @height = 297
  END

	declare @preview_url as nvarchar(max) = (
		(select [value] from dbo.s_config where [name] = N'preview_url') +
		N'page/' +
		cast(@creative_nmb as nvarchar(16)) +
		N'?token=a&version=' +
		cast(@creative_version_nmb as nvarchar(16))
	);

	if @width <> 0 and @height <> 0 and (select put_creatives_to_rtb from dbo.t_template_version where nmb = @template_version_nmb) = 1
	begin
		select 
			0 as assembly_nmb, 
			@width width,
			@height height, 
			@preview_url preview_url
	end else begin
		select TOP 1  -- берём одну рандомную сборку для показа в премодерации
			nmb as assembly_nmb, 
			case when width <> 0 then width else @width end width,
			case when height <> 0 then height else @height end height, 
			@preview_url + '&rtb_assembly=' + cast(nmb as nvarchar(16)) as preview_url
		from (
			select nmb, width, height
			from dbo.t_assembly
			where creative_version_nmb = @creative_version_nmb and is_test = 1
			union all
			select a.nmb, a.width, a.height
			from 
				(select * from dbo.t_template_version where template_test_assembly_set_nmb is not null) tv
				join (select * from dbo.t_assembly where is_test = 1) as a on a.template_test_assembly_set_nmb = tv.template_test_assembly_set_nmb
			where
				tv.nmb = @template_version_nmb
				and ((a.width = 0 and @width <> 0) or (@width = 0 and a.width <> 0) or a.width = @width) 
				and ((a.height = 0 and @height <> 0) or (@height = 0 and a.height <> 0) or a.height = @height)
		) as a
		order by newid();
	end
end;
go

grant execute on dbo.sp_moderation_creative_get_test_assembly_list to admin_zone_role;
go
