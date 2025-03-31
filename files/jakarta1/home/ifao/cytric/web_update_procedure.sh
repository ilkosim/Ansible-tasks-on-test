java net.ifao.application.data.ExecuteSql -rows MAX "
CREATE OR REPLACE PROCEDURE WEB_UPDATE_PROCEDURE (updated_objects OUT cytric_direct_update.tmscursor) IS
BEGIN

   insert into tmp_sys_oids 
     select t2.sys_oid from Tlabel_t t1, DivTypeCopy_t t2
     where t1.name_c = 'stanislava-axml'
     and t1.sys_oid = t2.parent_c
     and t2.divisionname_c in ('BBBB');
 
   delete from TLabelDivisionInfoSeq_t
     where sys_val in (select sys_oid from tmp_sys_oids);
 
   update TLabelDivisionInfoSeq_t a
     set a.sys_key = (select count(a.sys_key) from TLabelDivisionInfoSeq_t b where b.sys_oid = a.sys_oid and b.sys_key < a.sys_key)
     where sys_oid in (select sys_oid from Tlabel_t where name_c = 'stanislava-axml');

	OPEN updated_objects FOR SELECT sys_oid FROM TLabel_t WHERE name_c = 'stanislava-axml';
END;
"
